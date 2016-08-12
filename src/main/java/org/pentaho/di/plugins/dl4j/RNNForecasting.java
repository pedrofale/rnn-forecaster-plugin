package org.pentaho.di.plugins.dl4j;

/**
 * Created by pedro on 08-08-2016.
 */
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import weka.core.BatchPredictor;
import weka.core.Instances;
import weka.core.SerializedObject;

/**
 * Applies a pre-built weka model (classifier or clusterer) to incoming rows and
 * appends predictions. Predictions can be a label (classification/clustering),
 * a number (regression), or a probability distribution over classes/clusters.
 * <p>
 *
 * Attributes that the Weka model was constructed from are automatically mapped
 * to incoming Kettle fields on the basis of name and type. Any attributes that
 * cannot be mapped due to type mismatch or not being present in the incoming
 * fields receive missing values when incoming Kettle rows are converted to
 * Weka's Instance format. Similarly, any values for string fields that have not
 * been seen during the training of the Weka model are converted to missing
 * values.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 */
public class RNNForecasting extends BaseStep implements StepInterface {

    private RNNForecastingMeta m_meta;
    private RNNForecastingData m_data;

    /** size of the batches of rows to be scored if the model is a batch scorer */
    private int m_batchScoringSize = RNNForecastingMeta.DEFAULT_steps_to_forecast;
    private List<Object[]> m_batch;

    /**
     * Creates a new <code>RNNForecasting</code> instance.
     *
     * @param stepMeta holds the step's meta data
     * @param stepDataInterface holds the step's temporary data
     * @param copyNr the number assigned to the step
     * @param transMeta meta data for the transformation
     * @param trans a <code>Trans</code> value
     */
    public RNNForecasting(StepMeta stepMeta, StepDataInterface stepDataInterface,
                       int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    private RNNForecastingModel setModel(String modelFileName)
            throws KettleException {

    /*
     * String modName = environmentSubstitute(modelFileName); File modelFile =
     * null; if (modName.startsWith("file:")) { //$NON-NLS-1$ try { modName =
     * modName.replace(" ", "%20"); //$NON-NLS-1$ //$NON-NLS-2$ modelFile = new
     * File(new java.net.URI(modName)); } catch (Exception ex) { throw new
     * KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
     * "RNNForecasting.Error.MalformedURIForModelFile"), ex); //$NON-NLS-1$ } }
     * else { modelFile = new File(modName); } if (!modelFile.exists()) { throw
     * new KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
     * "RNNForecasting.Error.NonExistentModelFile", modName)); //$NON-NLS-1$ }
     */

        // Load the model
        RNNForecastingModel model = null;
        try {
            model = RNNForecastingData.loadSerializedModel(modelFileName,
                    getLogChannel(), this);
            m_data.setModel(model);

        } catch (Exception ex) {
            throw new KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
                    "RNNForecasting.Error.ProblemDeserializingModel"), ex); //$NON-NLS-1$
        }
        return model;
    }

    /**
     * Process an incoming row of data.
     *
     * @param smi a <code>StepMetaInterface</code> value
     * @param sdi a <code>StepDataInterface</code> value
     * @return a <code>boolean</code> value
     * @exception KettleException if an error occurs
     */
    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi)
            throws KettleException {

        m_meta = (RNNForecastingMeta) smi;
        m_data = (RNNForecastingData) sdi;

        Object[] r = getRow();

        // No more rows to be read -- make forecast
        if (r == null) {
            try {
                outputBatchRows();
            } catch (Exception ex) {
                throw new KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
                        "RNNForecasting.Error.ProblemWhileGettingPredictionsForBatch"), ex); //$NON-NLS-1$
            }

            m_data.getModel().done();

            setOutputDone();
            return false;
        }

        // Handle the first row
        if (first) {
            first = false;

            m_data.setOutputRowMeta(getInputRowMeta().clone());
            if (m_meta.getModel() == null
                    || !Const.isEmpty(m_meta.getSerializedModelFileName())) {
                // If we don't have a model, or a file name is set, then load from file

                // Check that we have a file to try and load a classifier from
                if (Const.isEmpty(m_meta.getSerializedModelFileName())) {
                    throw new KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
                            "RNNForecasting.Error.NoFilenameToLoadModelFrom")); //$NON-NLS-1$
                }

                setModel(m_meta.getSerializedModelFileName());
            } else if (m_meta.getModel() != null) {
                // copy the primary model over to the data class
                try {
                    SerializedObject so = new SerializedObject(m_meta.getModel());
                    RNNForecastingModel defaultModel = (RNNForecastingModel) so.getObject();

                    m_data.setModel(defaultModel);
                } catch (Exception ex) {
                    throw new KettleException(ex);
                }
            }

            // Check the input row meta data against the instances
            // header that the classifier was trained with
            try {
                Instances header = m_data.getModel().getHeader();
                m_data.mapIncomingRowMetaData(header, getInputRowMeta(), log);
            } catch (Exception ex) {
                throw new KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
                        "RNNForecasting.Error.IncomingDataFormatDoesNotMatchModel"), ex); //$NON-NLS-1$
            }

            // Determine the output format
            m_meta.getFields(m_data.getOutputRowMeta(), getStepname(), null, null,
                    this);

            if (!Const.isEmpty(m_meta.getStepsToForecast())) {
                try {
                    String bss = environmentSubstitute(m_meta.getStepsToForecast());
                    m_batchScoringSize = Integer.parseInt(bss);
                } catch (NumberFormatException ex) {
                    String modelPreferred = environmentSubstitute(((BatchPredictor) m_meta
                            .getModel().getModel()).getBatchSize());

                    boolean sizeOk = false;
                    if (!Const.isEmpty(modelPreferred)) {
                        logBasic(BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecasting.Message.UnableToParseBatchScoringSize", //$NON-NLS-1$
                                modelPreferred));
                        try {
                            m_batchScoringSize = Integer.parseInt(modelPreferred);
                            sizeOk = true;
                        } catch (NumberFormatException e) {
                        }
                    }

                    if (!sizeOk) {
                        logBasic(BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecasting.Message.UnableToParseBatchScoringSizeDefault", //$NON-NLS-1$
                                RNNForecastingMeta.DEFAULT_steps_to_forecast));

                        m_batchScoringSize = RNNForecastingMeta.DEFAULT_steps_to_forecast;
                    }
                }
            }

            m_batch = new ArrayList<Object[]>();

        } // end (if first)

        // Make prediction for row using model
        try {
            try {
                // add current row to batch
                m_batch.add(r);

            } catch (Exception ex) {
                throw new KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
                        "RNNForecasting.Error.ErrorGettingBatchPredictions"), ex); //$NON-NLS-1$
            }
        } catch (Exception ex) {
            throw new KettleException(BaseMessages.getString(RNNForecastingMeta.PKG,
                    "RNNForecasting.Error.UnableToMakePredictionForRow", getLinesRead()), ex); //$NON-NLS-1$
        }

        if (log.isRowLevel()) {
            log.logRowlevel(toString(), "Read row #" + getLinesRead() + " : " + r); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (checkFeedback(getLinesRead())) {
            logBasic("Linenr " + getLinesRead()); //$NON-NLS-1$
        }
        return true;
    }

    protected void outputBatchRows() throws Exception {
        // get predictions for the batch
        Object[][] outputRows = m_data.generateForecast(getInputRowMeta(),
                m_data.getOutputRowMeta(), m_batch, m_meta);

        if (log.isDetailed()) {
            logDetailed(BaseMessages.getString(RNNForecastingMeta.PKG,
                    "RNNForecasting.Message.PredictingBatch")); //$NON-NLS-1$
        }

        // output the rows
        for (Object[] row : outputRows) {
            putRow(m_data.getOutputRowMeta(), row);
        }

        // reset batch
        m_batch.clear();
    }

    /**
     * Initialize the step.
     *
     * @param smi a <code>StepMetaInterface</code> value
     * @param sdi a <code>StepDataInterface</code> value
     * @return a <code>boolean</code> value
     */
    @Override
    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        m_meta = (RNNForecastingMeta) smi;
        m_data = (RNNForecastingData) sdi;

        if (super.init(smi, sdi)) {
            return true;
        }
        return false;
    }
}
