package org.pentaho.di.plugins.dl4j;

/**
 * Created by pedro on 08-08-2016.
 */

import org.apache.commons.vfs2.FileObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.vfs.ui.VfsFileChooserDialog;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.xml.XStream;

import java.io.Serializable;

/**
 * The UI class for the RNNForecasting transform
 *
 * @author Pedro Ferreira (pferreira{[at]}pentaho.org)
 * @version 1.0
 */
public class RNNForecastingDialog extends BaseStepDialog implements
        StepDialogInterface {

    /** various UI bits and pieces for the dialog */
    private Label m_wlStepname;
    private Text m_wStepname;
    private FormData m_fdlStepname;
    private FormData m_fdStepname;

    private FormData m_fdTabFolder;
    private FormData m_fdFileComp, m_fdFieldsComp, m_fdModelComp;

    /** The tabs of the dialog */
    private CTabFolder m_wTabFolder;
    private CTabItem m_wFileTab, m_wFieldsTab, m_wModelTab;

    /** label for the file name field */
    private Label m_wlFilename;

    /** file name field */
    private FormData m_fdlFilename, m_fdbFilename, m_fdFilename;

    /** Browse file button */
    private Button m_wbFilename;

    /** Combines text field with widget to insert environment variable */
    private TextVar m_wFilename;

    /** TextVar for batch sizes to be pushed to BatchPredictors */
    private TextVar m_stepsToForecastText;

    /** the text area for the model */
    private Text m_wModelText;
    private FormData m_fdModelText;

    /** the text area for the fields mapping */
    private Text m_wMappingText;
    private FormData m_fdMappingText;

    /**
     * meta data for the step. A copy is made so that changes, in terms of choices
     * made by the user, can be detected.
     */
    private final RNNForecastingMeta m_currentMeta;
    private final RNNForecastingMeta m_originalMeta;

    public RNNForecastingDialog(Shell parent, Object in, TransMeta tr, String sname) {

        super(parent, (BaseStepMeta) in, tr, sname);

        // The order here is important...
        // m_currentMeta is looked at for changes
        m_currentMeta = (RNNForecastingMeta) in;
        m_originalMeta = (RNNForecastingMeta) m_currentMeta.clone();
    }

    /**
     * Open the dialog
     *
     * @return the step name
     */
    public String open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);

        props.setLook(shell);
        setShellImage(shell, m_currentMeta);

        // used to listen to a text field (m_wStepname)
        ModifyListener lsMod = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                m_currentMeta.setChanged();
            }
        };

        changed = m_currentMeta.hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "RNNForecastingDialog.Shell.Title")); //$NON-NLS-1$

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        // Stepname line
        m_wlStepname = new Label(shell, SWT.RIGHT);
        m_wlStepname.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "RNNForecastingDialog.StepName.Label")); //$NON-NLS-1$
        props.setLook(m_wlStepname);

        m_fdlStepname = new FormData();
        m_fdlStepname.left = new FormAttachment(0, 0);
        m_fdlStepname.right = new FormAttachment(middle, -margin);
        m_fdlStepname.top = new FormAttachment(0, margin);
        m_wlStepname.setLayoutData(m_fdlStepname);
        m_wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        m_wStepname.setText(stepname);
        props.setLook(m_wStepname);
        m_wStepname.addModifyListener(lsMod);

        // format the text field
        m_fdStepname = new FormData();
        m_fdStepname.left = new FormAttachment(middle, 0);
        m_fdStepname.top = new FormAttachment(0, margin);
        m_fdStepname.right = new FormAttachment(100, 0);
        m_wStepname.setLayoutData(m_fdStepname);

        m_wTabFolder = new CTabFolder(shell, SWT.BORDER);
        props.setLook(m_wTabFolder, Props.WIDGET_STYLE_TAB);
        m_wTabFolder.setSimple(false);

        // Start of the file tab
        m_wFileTab = new CTabItem(m_wTabFolder, SWT.NONE);
        m_wFileTab.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "RNNForecastingDialog.FileTab.TabTitle")); //$NON-NLS-1$

        Composite wFileComp = new Composite(m_wTabFolder, SWT.NONE);
        props.setLook(wFileComp);

        FormLayout fileLayout = new FormLayout();
        fileLayout.marginWidth = 3;
        fileLayout.marginHeight = 3;
        wFileComp.setLayout(fileLayout);

        // Filename line
        m_wlFilename = new Label(wFileComp, SWT.RIGHT);
        m_wlFilename.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "RNNForecastingDialog.Filename.Label")); //$NON-NLS-1$
        props.setLook(m_wlFilename);
        m_fdlFilename = new FormData();
        m_fdlFilename.left = new FormAttachment(0, 0);
        m_fdlFilename.top = new FormAttachment(0, margin);
        m_fdlFilename.right = new FormAttachment(middle, -margin);
        m_wlFilename.setLayoutData(m_fdlFilename);

        // file browse button
        m_wbFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
        props.setLook(m_wbFilename);
        m_wbFilename.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "System.Button.Browse")); //$NON-NLS-1$
        m_fdbFilename = new FormData();
        m_fdbFilename.right = new FormAttachment(100, 0);
        m_fdbFilename.top = new FormAttachment(0, 0);
        m_wbFilename.setLayoutData(m_fdbFilename);

        // combined text field and env variable widget
        m_wFilename = new TextVar(transMeta, wFileComp, SWT.SINGLE | SWT.LEFT
                | SWT.BORDER);
        props.setLook(m_wFilename);
        m_wFilename.addModifyListener(lsMod);
        m_fdFilename = new FormData();
        m_fdFilename.left = new FormAttachment(middle, 0);
        m_fdFilename.top = new FormAttachment(0, margin);
        m_fdFilename.right = new FormAttachment(m_wbFilename, -margin);
        m_wFilename.setLayoutData(m_fdFilename);

        // number of steps to forecast line
        Label stepsLab = new Label(wFileComp, SWT.RIGHT);
        stepsLab.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "RNNForecastindDialog.StepsToForecast.Label")); //$NON-NLS-1$
        props.setLook(stepsLab);
        FormData fdd = new FormData();
        fdd.left = new FormAttachment(0, 0);
        fdd.top = new FormAttachment(m_wFilename, margin);
        fdd.right = new FormAttachment(middle, -margin);
        stepsLab.setLayoutData(fdd);

        m_stepsToForecastText = new TextVar(transMeta, wFileComp, SWT.SINGLE
                | SWT.LEFT | SWT.BORDER);
        props.setLook(m_stepsToForecastText);
        m_stepsToForecastText.addModifyListener(lsMod);
        fdd = new FormData();
        fdd.left = new FormAttachment(middle, 0);
        fdd.top = new FormAttachment(m_wFilename, margin);
        fdd.right = new FormAttachment(100, 0);
        m_stepsToForecastText.setLayoutData(fdd);
        m_stepsToForecastText.setEnabled(true);

        m_fdFileComp = new FormData();
        m_fdFileComp.left = new FormAttachment(0, 0);
        m_fdFileComp.top = new FormAttachment(0, 0);
        m_fdFileComp.right = new FormAttachment(100, 0);
        m_fdFileComp.bottom = new FormAttachment(100, 0);
        wFileComp.setLayoutData(m_fdFileComp);

        wFileComp.layout();
        m_wFileTab.setControl(wFileComp);

        // Fields mapping tab
        m_wFieldsTab = new CTabItem(m_wTabFolder, SWT.NONE);
        m_wFieldsTab.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "RNNForecastingDialog.FieldsTab.TabTitle")); //$NON-NLS-1$

        FormLayout fieldsLayout = new FormLayout();
        fieldsLayout.marginWidth = 3;
        fieldsLayout.marginHeight = 3;

        Composite wFieldsComp = new Composite(m_wTabFolder, SWT.NONE);
        props.setLook(wFieldsComp);
        wFieldsComp.setLayout(fieldsLayout);

        // body of tab to be a scrolling text area
        // to display the mapping
        m_wMappingText = new Text(wFieldsComp, SWT.MULTI | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        m_wMappingText.setEditable(false);
        FontData fontd = new FontData("Courier New", 12, SWT.NORMAL); //$NON-NLS-1$
        m_wMappingText.setFont(new Font(getParent().getDisplay(), fontd));

        props.setLook(m_wMappingText);
        // format the fields mapping text area
        m_fdMappingText = new FormData();
        m_fdMappingText.left = new FormAttachment(0, 0);
        m_fdMappingText.top = new FormAttachment(0, margin);
        m_fdMappingText.right = new FormAttachment(100, 0);
        m_fdMappingText.bottom = new FormAttachment(100, 0);
        m_wMappingText.setLayoutData(m_fdMappingText);

        m_fdFieldsComp = new FormData();
        m_fdFieldsComp.left = new FormAttachment(0, 0);
        m_fdFieldsComp.top = new FormAttachment(0, 0);
        m_fdFieldsComp.right = new FormAttachment(100, 0);
        m_fdFieldsComp.bottom = new FormAttachment(100, 0);
        wFieldsComp.setLayoutData(m_fdFieldsComp);

        wFieldsComp.layout();
        m_wFieldsTab.setControl(wFieldsComp);

        // Model display tab
        m_wModelTab = new CTabItem(m_wTabFolder, SWT.NONE);
        m_wModelTab.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "RNNForecastingDialog.ModelTab.TabTitle")); //$NON-NLS-1$

        FormLayout modelLayout = new FormLayout();
        modelLayout.marginWidth = 3;
        modelLayout.marginHeight = 3;

        Composite wModelComp = new Composite(m_wTabFolder, SWT.NONE);
        props.setLook(wModelComp);
        wModelComp.setLayout(modelLayout);

        // body of tab to be a scrolling text area
        // to display the pre-learned model

        m_wModelText = new Text(wModelComp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL
                | SWT.H_SCROLL);
        m_wModelText.setEditable(false);
        fontd = new FontData("Courier New", 12, SWT.NORMAL); //$NON-NLS-1$
        m_wModelText.setFont(new Font(getParent().getDisplay(), fontd));

        props.setLook(m_wModelText);
        // format the model text area
        m_fdModelText = new FormData();
        m_fdModelText.left = new FormAttachment(0, 0);
        m_fdModelText.top = new FormAttachment(0, margin);
        m_fdModelText.right = new FormAttachment(100, 0);
        m_fdModelText.bottom = new FormAttachment(100, 0);
        m_wModelText.setLayoutData(m_fdModelText);

        m_fdModelComp = new FormData();
        m_fdModelComp.left = new FormAttachment(0, 0);
        m_fdModelComp.top = new FormAttachment(0, 0);
        m_fdModelComp.right = new FormAttachment(100, 0);
        m_fdModelComp.bottom = new FormAttachment(100, 0);
        wModelComp.setLayoutData(m_fdModelComp);

        wModelComp.layout();
        m_wModelTab.setControl(wModelComp);

        m_fdTabFolder = new FormData();
        m_fdTabFolder.left = new FormAttachment(0, 0);
        m_fdTabFolder.top = new FormAttachment(m_wStepname, margin);
        m_fdTabFolder.right = new FormAttachment(100, 0);
        m_fdTabFolder.bottom = new FormAttachment(100, -50);
        m_wTabFolder.setLayoutData(m_fdTabFolder);

        // Buttons inherited from BaseStepDialog
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(RNNForecastingMeta.PKG, "System.Button.OK")); //$NON-NLS-1$

        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(RNNForecastingMeta.PKG,
                "System.Button.Cancel")); //$NON-NLS-1$

        setButtonPositions(new Button[] { wOK, wCancel }, margin, m_wTabFolder);

        // Add listeners
        lsCancel = new Listener() {
            public void handleEvent(Event e) {
                cancel();
            }
        };
        lsOK = new Listener() {
            public void handleEvent(Event e) {
                ok();
            }
        };

        wCancel.addListener(SWT.Selection, lsCancel);
        wOK.addListener(SWT.Selection, lsOK);

        lsDef = new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                ok();
            }
        };

        m_wStepname.addSelectionListener(lsDef);

        // Detect X or ALT-F4 or something that kills this window...
        shell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent e) {
                cancel();
            }
        });

        // Whenever something changes, set the tooltip to the expanded version:
        m_wFilename.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                m_wFilename.setToolTipText(transMeta.environmentSubstitute(m_wFilename
                        .getText()));
            }
        });

        // listen to the file name text box and try to load a model
        // if the user presses enter
        m_wFilename.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (!loadModel()) {
                    log.logError(BaseMessages.getString(RNNForecastingMeta.PKG,
                            "RNNForecastingDialog.Log.FileLoadingError")); //$NON-NLS-1$
                }
            }
        });

        m_wbFilename.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.SAVE);
                String[] extensions = null;
                String[] filterNames = null;
                if (XStream.isPresent()) {
                    extensions = new String[3];
                    filterNames = new String[3];
                    extensions[0] = "*.model"; //$NON-NLS-1$
                    filterNames[0] = BaseMessages.getString(RNNForecastingMeta.PKG,
                            "RNNForecastingDialog.FileType.ModelFileBinary"); //$NON-NLS-1$
                    extensions[1] = "*.xstreammodel"; //$NON-NLS-1$
                    filterNames[1] = BaseMessages.getString(RNNForecastingMeta.PKG,
                            "RNNForecastingDialog.FileType.ModelFileXML"); //$NON-NLS-1$
                    extensions[2] = "*"; //$NON-NLS-1$
                    filterNames[2] = BaseMessages.getString(RNNForecastingMeta.PKG,
                            "System.FileType.AllFiles"); //$NON-NLS-1$
                } else {
                    extensions = new String[2];
                    filterNames = new String[2];
                    extensions[0] = "*.model"; //$NON-NLS-1$
                    filterNames[0] = BaseMessages.getString(RNNForecastingMeta.PKG,
                            "RNNForecastingDialog.FileType.ModelFileBinary"); //$NON-NLS-1$
                    extensions[1] = "*"; //$NON-NLS-1$
                    filterNames[1] = BaseMessages.getString(RNNForecastingMeta.PKG,
                            "System.FileType.AllFiles"); //$NON-NLS-1$
                }
                dialog.setFilterExtensions(extensions);
                if (m_wFilename.getText() != null) {
                    dialog.setFileName(transMeta.environmentSubstitute(m_wFilename
                            .getText()));
                }
                dialog.setFilterNames(filterNames);

                if (dialog.open() != null) {

                    m_wFilename.setText(dialog.getFilterPath()
                            + System.getProperty("file.separator") + dialog.getFileName()); //$NON-NLS-1$
                }
            }
        });

        m_wTabFolder.setSelection(0);

        // Set the shell size, based upon previous time...
        setSize();

        getData();

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        return stepname;
    }

    /**
     * Load the model.
     */
    private boolean loadModel() {
        String filename = m_wFilename.getText();
        if (Const.isEmpty(filename)) {
            return false;
        }

        boolean success = false;

        // if (!Const.isEmpty(filename) && modelFile.exists()) {
        try {
            if (!Const.isEmpty(filename)
                    && RNNForecastingData.modelFileExists(filename, transMeta)) {

                RNNForecastingModel tempM = RNNForecastingData.loadSerializedModel(filename,
                        log, transMeta);
                m_wModelText.setText(tempM.toString());

                m_currentMeta.setModel(tempM);

                // see if we can find a previous step and set up the
                // mappings
                mappingString(tempM);
                success = true;

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.logError(BaseMessages.getString(RNNForecastingMeta.PKG,
                    "RNNForecastingDialog.Log.FileLoadingError"), ex); //$NON-NLS-1$
        }

        return success;
    }

    /**
     * Build a string that shows the mappings between Weka attributes and incoming
     * Kettle fields.
     *
     * @param model a <code>RNNForecastingModel</code> value
     */
    private void mappingString(RNNForecastingModel model) {

        try {
            StepMeta stepMetaTemp = transMeta.findStep(stepname);
            if (stepMetaTemp != null) {
                RowMetaInterface rowM = transMeta.getPrevStepFields(stepMetaTemp);
                Instances header = model.getHeader();
                int[] mappings = RNNForecastingData.findMappings(header, rowM);

                StringBuffer result = new StringBuffer(header.numAttributes() * 10);

                int maxLength = 0;
                for (int i = 0; i < header.numAttributes(); i++) {
                    if (header.attribute(i).name().length() > maxLength) {
                        maxLength = header.attribute(i).name().length();
                    }
                }
                maxLength += 12; // length of " (nominal)"/" (numeric)"

                int minLength = 16; // "Model attributes".length()
                String headerS = BaseMessages.getString(RNNForecastingMeta.PKG,
                        "RNNForecastingDialog.Mapping.ModelAttsHeader"); //$NON-NLS-1$
                String sep = "----------------"; //$NON-NLS-1$

                if (maxLength < minLength) {
                    maxLength = minLength;
                }
                headerS = getFixedLengthString(headerS, ' ', maxLength);
                sep = getFixedLengthString(sep, '-', maxLength);
                sep += "\t    ----------------\n"; //$NON-NLS-1$
                headerS += "\t    " //$NON-NLS-1$
                        + BaseMessages.getString(RNNForecastingMeta.PKG,
                        "RNNForecastingDialog.Mapping.IncomingFields") + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
                result.append(headerS);
                result.append(sep);

                for (int i = 0; i < header.numAttributes(); i++) {
                    Attribute temp = header.attribute(i);
                    String attName = "("; //$NON-NLS-1$
                    if (temp.isNumeric()) {
                        attName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecastingDialog.Mapping.Numeric") + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    } else if (temp.isNominal()) {
                        attName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecastingDialog.Mapping.Nominal") + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    } else if (temp.isString()) {
                        attName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecastingDialog.Mapping.String") + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    } else if (temp.isDate()) {
                        attName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecastingDialog.Mapping.Date") + ")";
                    }
                    attName += (" " + temp.name()); //$NON-NLS-1$

                    attName = getFixedLengthString(attName, ' ', maxLength);
                    attName += "\t--> "; //$NON-NLS-1$
                    result.append(attName);
                    String inFieldNum = ""; //$NON-NLS-1$
                    if (mappings[i] == RNNForecastingData.NO_MATCH) {
                        inFieldNum += "- "; //$NON-NLS-1$
                        result.append(inFieldNum
                                + BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecastingDialog.Mapping.MissingNoMatch") + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    } else if (mappings[i] == RNNForecastingData.TYPE_MISMATCH) {
                        inFieldNum += (rowM.indexOfValue(temp.name()) + 1) + " "; //$NON-NLS-1$
                        result.append(inFieldNum
                                + BaseMessages.getString(RNNForecastingMeta.PKG,
                                "RNNForecastingDialog.Mapping.MissingTypeMismatch") + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    } else {
                        ValueMetaInterface tempField = rowM.getValueMeta(mappings[i]);
                        String fieldName = "" + (mappings[i] + 1) + " ("; //$NON-NLS-1$ //$NON-NLS-2$
                        if (tempField.isBoolean()) {
                            fieldName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                    "RNNForecastingDialog.Mapping.Boolean") + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                        } else if (tempField.isNumeric()) {
                            fieldName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                    "RNNForecastingDialog.Mapping.Numeric") + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                        } else if (tempField.isString()) {
                            fieldName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                    "RNNForecastingDialog.Mapping.String") + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                        } else if (tempField.isDate()) {
                            fieldName += BaseMessages.getString(RNNForecastingMeta.PKG,
                                    "RNNForecastingDialog.Mapping.Date") + ")";
                        }
                        fieldName += " " + tempField.getName(); //$NON-NLS-1$
                        result.append(fieldName + "\n"); //$NON-NLS-1$
                    }
                }

                // set the text of the text area in the Mappings tab
                m_wMappingText.setText(result.toString());
            }
        } catch (KettleException e) {
            log.logError(BaseMessages.getString(RNNForecastingMeta.PKG,
                    "RNNForecastingDialog.Log.UnableToFindInput")); //$NON-NLS-1$
            return;
        }
    }

    /**
     * Grab data out of the step meta object
     */
    public void getData() {

        if (m_currentMeta.getSerializedModelFileName() != null) {
            m_wFilename.setText(m_currentMeta.getSerializedModelFileName());
        }

        if (!Const.isEmpty(m_currentMeta.getStepsToForecast())) {
            m_stepsToForecastText.setText(m_currentMeta.getStepsToForecast());
        }

        // Grab model if it is available (and we are not reading model file
        // names from a field in the incoming data
        RNNForecastingModel tempM = m_currentMeta.getModel();
        if (tempM != null) {
            m_wModelText.setText(tempM.toString());

            // Grab mappings if available
            mappingString(tempM);
        } else {
            // try loading the model
            loadModel();
        }
        // }
    }

    private void cancel() {
        stepname = null;
        m_currentMeta.setChanged(changed);

        // revert to original model
        RNNForecastingModel temp = m_originalMeta.getModel();
        m_currentMeta.setModel(temp);
        dispose();
    }

    private void ok() {
        if (Const.isEmpty(m_wStepname.getText())) {
            return;
        }

        stepname = m_wStepname.getText(); // return value

        if (!Const.isEmpty(m_wFilename.getText())) {
            m_currentMeta.setSerializedModelFileName(m_wFilename.getText());
        } else {
            loadModel();
            m_currentMeta.setSerializedModelFileName(null);
        }

        if (!Const.isEmpty(m_stepsToForecastText.getText())) {
            m_currentMeta.setStepsToForecast(m_stepsToForecastText.getText());
        }

        if (!m_originalMeta.equals(m_currentMeta)) {
            m_currentMeta.setChanged();
            changed = m_currentMeta.hasChanged();
        }

        dispose();
    }

    /**
     * Helper method to pad/truncate strings
     *
     * @param s String to modify
     * @param pad character to pad with
     * @param len length of final string
     * @return final String
     */
    private static String getFixedLengthString(String s, char pad, int len) {

        String padded = null;
        if (len <= 0) {
            return s;
        }
        // truncate?
        if (s.length() >= len) {
            return s.substring(0, len);
        } else {
            char[] buf = new char[len - s.length()];
            for (int j = 0; j < len - s.length(); j++) {
                buf[j] = pad;
            }
            padded = s + new String(buf);
        }

        return padded;
    }
}
