package org.pentaho.di.plugins.dl4j;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import weka.core.SerializedObject;

/**
 * Contains the meta data for the RNNForecasting step.
 *
 * @author Pedro Ferreira (pferreira{[at]}pentaho{[dot]}org)
 * @version 1.0
 */
@Step(id = "RNNForecasting", image = "rnnforecaster.svg", name = "RNN Forecasting",
        description = "Appends predictions from a pre-built RNN model in Weka", categoryDescription = "Data Mining",
        documentationUrl = "http://wiki.pentaho.com/display/DATAMINING/Using+the+Weka+Scoring+Plugin")
public class RNNForecastingMeta extends BaseStepMeta implements StepMetaInterface {

    private static final long serialVersionUID = -7363244115597574265L;

    protected static Class<?> PKG = RNNForecastingMeta.class;

    public static final String XML_TAG = "rnn_forecasting"; //$NON-NLS-1$

    /** File name of the serialized Weka model to load/import */
    private String m_modelFileName;

    /** Number of time steps to forecast from the end of training data */
    private String m_stepsToForecast;
    public static final int DEFAULT_steps_to_forecast = 1;

    /**
     *  Whether to clear previous RNN state
     */
    private boolean m_clearPrevState;

    /** Holds the actual Weka model (forecaster) */
    private RNNForecastingModel m_model;

    /**
     * Set the number of time steps to forecast
     *
     * @param numberOfSteps the file name
     */
    public void setStepsToForecast(String numberOfSteps) {
        m_stepsToForecast = numberOfSteps;
    }

    /**
     * Get the number of time steps to forecast
     *
     * @return number of steps to forecast
     */
    public String getStepsToForecast() {
        return m_stepsToForecast;
    }

    /**
     * Set the file name of the serialized Weka model to load/import from
     *
     * @param mfile the file name
     */
    public void setSerializedModelFileName(String mfile) {
        m_modelFileName = mfile;
    }

    /**
     * Get the filename of the serialized Weka model to load/import from
     *
     * @return the file name
     */
    public String getSerializedModelFileName() {
        return m_modelFileName;
    }

    /**
     * Set whether to clear previous RNN state or not
     */
    public void setClearPreviousState(boolean clearPreviousState) {
        m_clearPrevState = clearPreviousState;
    }

    /**
     * Set whether to clear previous RNN state or not
     */
    public boolean getClearPreviousState() {
        return m_clearPrevState;
    }

    /**
     * Creates a new <code>RNNForecastingMeta</code> instance.
     */
    public RNNForecastingMeta() {
        super(); // allocate BaseStepMeta
    }

    /**
     * Set the Weka model
     *
     * @param model a <code>RNNForecastingModel</code> that encapsulates the actual
     *          Weka model (Forecaster)
     */
    public void setModel(RNNForecastingModel model) {
        m_model = model;
    }

    /**
     * Get the Weka model
     *
     * @return a <code>RNNForecastingModel</code> that encapsulates the actual Weka
     *         model (Forecaster)
     */
    public RNNForecastingModel getModel() {
        return m_model;
    }


    protected String getXML(boolean logging) {
        StringBuffer retval = new StringBuffer(100);

        retval.append("<" + XML_TAG + ">"); //$NON-NLS-1$ //$NON-NLS-2$

        if (!Const.isEmpty(m_stepsToForecast)) {
            retval.append(XMLHandler.addTagValue("steps_to_forecast", //$NON-NLS-1$
                    m_stepsToForecast));
        }

        RNNForecastingModel temp = m_model;

        // can we save the model as XML?
        if (temp != null && Const.isEmpty(m_modelFileName)) {

            try {
                // Convert model to base64 encoding
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(bao);
                ObjectOutputStream oo = new ObjectOutputStream(bos);
                oo.writeObject(temp);
                oo.flush();
                byte[] model = bao.toByteArray();
                String base64model = XMLHandler
                        .addTagValue("weka_scoring_model", model); //$NON-NLS-1$
                String modType = ""; //$NON-NLS-1$ //$NON-NLS-2$
                System.out.println("Serializing " + modType + " model."); //$NON-NLS-1$ //$NON-NLS-2$
                System.out.println(BaseMessages.getString(PKG,
                        "RNNForecastingMeta.Log.SizeOfModel") + " " + base64model.length()); //$NON-NLS-1$ //$NON-NLS-2$

                retval.append(base64model);
                oo.close();
            } catch (Exception ex) {
                System.out.println(BaseMessages.getString(PKG,
                        "RNNForecastingMeta.Log.Base64SerializationProblem")); //$NON-NLS-1$
            }
        } else {
            if (!Const.isEmpty(m_modelFileName)) {

                if (logging) {
                    logDetailed(BaseMessages.getString(PKG,
                            "RNNForecastingMeta.Log.ModelSourcedFromFile") + " " + m_modelFileName); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            // save the model file name
            retval.append(XMLHandler.addTagValue("model_file_name", m_modelFileName)); //$NON-NLS-1$
        }

        retval.append("</" + XML_TAG + ">"); //$NON-NLS-1$ //$NON-NLS-2$
        return retval.toString();
    }

    /**
     * Return the XML describing this (configured) step
     *
     * @return a <code>String</code> containing the XML
     */
    @Override
    public String getXML() {
        return getXML(true);
    }

    /**
     * Check for equality
     *
     * @param obj an <code>Object</code> to compare with
     * @return true if equal to the supplied object
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && (obj.getClass().equals(this.getClass()))) {
            RNNForecastingMeta m = (RNNForecastingMeta) obj;
            return (getXML(false) == m.getXML(false));
        }

        return false;
    }

    /**
     * Hash code method
     *
     * @return the hash code for this object
     */
    @Override
    public int hashCode() {
        return getXML(false).hashCode();
    }

    /**
     * Clone this step's meta data
     *
     * @return the cloned meta data
     */
    @Override
    public Object clone() {
        RNNForecastingMeta retval = (RNNForecastingMeta) super.clone();
        // deep copy the model (if any)
        if (m_model != null) {
            try {
                SerializedObject so = new SerializedObject(m_model);
                RNNForecastingModel copy = (RNNForecastingModel) so.getObject();
                retval.setModel(copy);
            } catch (Exception ex) {
                logError(BaseMessages.getString(PKG,
                        "RNNForecastingMeta.Log.DeepCopyingError")); //$NON-NLS-1$
            }
        }

        return retval;
    }

    public void setDefault() {
        m_modelFileName = null;
    }

    /**
     * Loads the meta data for this (configured) step from XML.
     *
     * @param stepnode the step to load
     * @exception KettleXMLException if an error occurs
     */
    public void loadXML(Node stepnode, List<DatabaseMeta> databases,
                        Map<String, Counter> counters) throws KettleXMLException {
        int nrModels = XMLHandler.countNodes(stepnode, XML_TAG);

        if (nrModels > 0) {
            Node wekanode = XMLHandler.getSubNodeByNr(stepnode, XML_TAG, 0);

            String temp = XMLHandler.getTagValue(wekanode, "file_name_from_field"); //$NON-NLS-1$

            m_stepsToForecast = XMLHandler.getTagValue(wekanode,
                    "steps_to_forecast"); //$NON-NLS-1$

            // try and get the XML-based model
            boolean success = false;
            try {
                String base64modelXML = XMLHandler.getTagValue(wekanode,
                        "weka_scoring_model"); //$NON-NLS-1$

                deSerializeBase64Model(base64modelXML);
                success = true;

                String modType = ""; //$NON-NLS-1$ //$NON-NLS-2$
                logBasic("Deserializing " + modType + " model."); //$NON-NLS-1$ //$NON-NLS-2$

                logDetailed(BaseMessages.getString(PKG,
                        "RNNForecastingMeta.Log.DeserializationSuccess")); //$NON-NLS-1$
            } catch (Exception ex) {
                success = false;
            }

            if (!success) {
                // fall back and try and grab a model file name
                m_modelFileName = XMLHandler.getTagValue(wekanode, "model_file_name"); //$NON-NLS-1$
            }
        }
    }

    protected void loadModelFile() throws Exception {
    /*
     * File modelFile = new File(m_modelFileName); if (modelFile.exists()) {
     */
        if (RNNForecastingData.modelFileExists(m_modelFileName, new Variables())) {
            logDetailed(BaseMessages.getString(PKG,
                    "RNNForecastingMeta.Message.LoadingModelFromFile")); //$NON-NLS-1$
            m_model = RNNForecastingData.loadSerializedModel(m_modelFileName,
                    getLog(), new Variables());
        }
    }

    protected void deSerializeBase64Model(String base64modelXML) throws Exception {
        byte[] model = XMLHandler.stringToBinary(base64modelXML);

        // now de-serialize
        ByteArrayInputStream bis = new ByteArrayInputStream(model);
        ObjectInputStream ois = new ObjectInputStream(bis);

        m_model = (RNNForecastingModel) ois.readObject();
        ois.close();
    }

    /**
     * Read this step's configuration from a repository
     *
     * @param rep the repository to access
     * @param id_step the id for this step
     * @exception KettleException if an error occurs
     */
    public void readRep(Repository rep, ObjectId id_step,
                        List<DatabaseMeta> databases, Map<String, Counter> counters)
            throws KettleException {

        // try and get a filename first as this overrides any model stored
        // in the repository
        boolean success = false;
        try {
            m_modelFileName = rep.getStepAttributeString(id_step, 0,
                    "model_file_name"); //$NON-NLS-1$
            success = true;
            if (m_modelFileName == null || Const.isEmpty(m_modelFileName)) {
                success = false;
            }
        } catch (KettleException ex) {
            success = false;
        }

        if (!success) {
            // try and get the model itself...
            try {
                String base64XMLModel = rep.getStepAttributeString(id_step, 0,
                        "rnn_forecasting_model"); //$NON-NLS-1$
                logDebug(BaseMessages.getString(PKG, "RNNForecastingMeta.Log.SizeOfModel") //$NON-NLS-1$
                        + " " + base64XMLModel.length()); //$NON-NLS-1$

                if (base64XMLModel != null && base64XMLModel.length() > 0) {
                    // try to de-serialize
                    deSerializeBase64Model(base64XMLModel);
                    success = true;
                } else {
                    success = false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                success = false;
            }
        }
    }

    /**
     * Save this step's meta data to a repository
     *
     * @param rep the repository to save to
     * @param id_transformation transformation id
     * @param id_step step id
     * @exception KettleException if an error occurs
     */
    public void saveRep(Repository rep, ObjectId id_transformation,
                        ObjectId id_step) throws KettleException {

        if (!Const.isEmpty(m_stepsToForecast)) {
            rep.saveStepAttribute(id_transformation, id_step, 0,
                    "steps_to_forecast", m_stepsToForecast); //$NON-NLS-1$
        }

        RNNForecastingModel temp = m_model;

        if (temp != null && Const.isEmpty(m_modelFileName)) {
            try {
                // Convert model to base64 encoding
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(bao);
                ObjectOutputStream oo = new ObjectOutputStream(bos);
                oo.writeObject(temp);
                oo.flush();
                byte[] model = bao.toByteArray();
                String base64XMLModel = KettleDatabaseRepository
                        .byteArrayToString(model);

                String modType = ""; //$NON-NLS-1$ //$NON-NLS-2$
                logDebug("Serializing " + modType + " model."); //$NON-NLS-1$ //$NON-NLS-2$

                rep.saveStepAttribute(id_transformation, id_step, 0,
                        "rnn_forecasting_model", base64XMLModel); //$NON-NLS-1$
                oo.close();
            } catch (Exception ex) {
                logError(BaseMessages.getString(PKG,
                        "RNNForecastingDialog.Log.Base64SerializationProblem"), ex); //$NON-NLS-1$
            }
        } else {
            // either XStream is not present or user wants to source from
            // file
            if (!Const.isEmpty(m_modelFileName)) {
                logBasic(BaseMessages.getString(PKG,
                        "RNNForecastingMeta.Log.ModelSourcedFromFile") + " " + m_modelFileName); //$NON-NLS-1$ //$NON-NLS-2$
            }

            rep.saveStepAttribute(id_transformation, id_step, 0, "model_file_name", //$NON-NLS-1$
                    m_modelFileName);
        }
    }

    /**
     * Generates row meta data to represent the fields output by this step
     *
     * @param row the meta data for the output produced
     * @param origin the name of the step to be used as the origin
     * @param info The input rows metadata that enters the step through the
     *          specified channels in the same order as in method getInfoSteps().
     *          The step metadata can then choose what to do with it: ignore it or
     *          not.
     * @param nextStep if this is a non-null value, it's the next step in the
     *          transformation. The one who's asking, the step where the data is
     *          targetted towards.
     * @param space not sure what this is :-)
     * @exception KettleStepException if an error occurs
     */
    @Override
    public void getFields(RowMetaInterface row, String origin,
                          RowMetaInterface[] info, StepMeta nextStep, VariableSpace space)
            throws KettleStepException{

        if (m_model == null && !Const.isEmpty(getSerializedModelFileName())) {
            // see if we can load from a file.

            String modName = getSerializedModelFileName();

            // if (!modelFile.exists()) {
            try {
                if (!RNNForecastingData.modelFileExists(modName, space)) {
                    throw new KettleStepException(BaseMessages.getString(PKG,
                            "RNNForecasting.Error.NonExistentModelFile")); //$NON-NLS-1$
                }

                RNNForecastingModel model = RNNForecastingData.loadSerializedModel(
                        m_modelFileName, getLog(), space);
                setModel(model);
            } catch (Exception ex) {
                throw new KettleStepException(BaseMessages.getString(PKG,
                        "RNNForecasting.Error.ProblemDeserializingModel"), ex); //$NON-NLS-1$
            }
        }

    }

    /**
     * Check the settings of this step and put findings in a remarks list.
     *
     * @param remarks the list to put the remarks in. see
     *          <code>org.pentaho.di.core.CheckResult</code>
     * @param transmeta the transform meta data
     * @param stepMeta the step meta data
     * @param prev the fields coming from a previous step
     * @param input the input step names
     * @param output the output step names
     * @param info the fields that are used as information by the step
     */
    public void check(List<CheckResultInterface> remarks, TransMeta transmeta,
                      StepMeta stepMeta, RowMetaInterface prev, String[] input,
                      String[] output, RowMetaInterface info) {

        CheckResult cr;

        if ((prev == null) || (prev.size() == 0)) {
            cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING,
                    "Not receiving any fields from previous steps!", stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK,
                    "Step is connected to previous one, receiving " + prev.size() //$NON-NLS-1$
                            + " fields", stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        }

        // See if we have input streams leading to this step!
        if (input.length > 0) {
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK,
                    "Step is receiving info from other steps.", stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,
                    "No input received from other steps!", stepMeta); //$NON-NLS-1$
            remarks.add(cr);
        }

        if (m_model == null) {
            if (!Const.isEmpty(m_modelFileName)) {
                File f = new File(m_modelFileName);
                if (!f.exists()) {
                    cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,
                            "Step does not have access to a " + "usable model!", stepMeta); //$NON-NLS-1$ //$NON-NLS-2$
                    remarks.add(cr);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.pentaho.di.trans.step.BaseStepMeta#getDialogClassName()
     */
    @Override
    public String getDialogClassName() {
        return "org.pentaho.di.plugins.dl4j.RNNForecastingDialog"; //$NON-NLS-1$
    }

    /**
     * Get the executing step, needed by Trans to launch a step.
     *
     * @param stepMeta the step info
     * @param stepDataInterface the step data interface linked to this step. Here
     *          the step can store temporary data, database connections, etc.
     * @param cnr the copy number to get.
     * @param tr the transformation info.
     * @param trans the launching transformation
     * @return a <code>StepInterface</code> value
     */
    public StepInterface getStep(StepMeta stepMeta,
                                 StepDataInterface stepDataInterface, int cnr, TransMeta tr, Trans trans) {

        return new RNNForecasting(stepMeta, stepDataInterface, cnr, tr, trans);
    }

    /**
     * Get a new instance of the appropriate data class. This data class
     * implements the StepDataInterface. It basically contains the persisting data
     * that needs to live on, even if a worker thread is terminated.
     *
     * @return a <code>StepDataInterface</code> value
     */
    public StepDataInterface getStepData() {

        return new RNNForecastingData();
    }
}
