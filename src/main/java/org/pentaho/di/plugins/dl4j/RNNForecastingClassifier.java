package org.pentaho.di.plugins.dl4j;

/**
 * Created by pedro on 08-08-2016.
 */

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.core.OverlayForecaster;
import weka.classifiers.timeseries.core.StateDependentPredictor;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.supervised.attribute.TSLagMaker;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Subclass of RNNForecastingModel that encapsulates a WekaForecaster.
 *
 * @author  Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */

class RNNForecastingClassifier extends RNNForecastingModel {

    // The encapsulated WekaForecaster
    private WekaForecaster m_model;

    /**
     * Creates a new <code>RNNForecastingWekaForecaster</code> instance.
     *
     * @param model the WekaForecaster
     */
    public RNNForecastingClassifier(Object model) {
        super(model);
    }

    /**
     * Set the WekaForecaster model
     *
     * @param model a WekaForecaster
     */
    public void setModel(Object model) {
        m_model = (WekaForecaster)model;
    }

    /**
     * Get the weka model
     *
     * @return the Weka model as an object
     */
    public WekaForecaster getModel() {
        return m_model;
    }

    /**
     * Set the base model
     *
     */
    public void loadBaseModel() throws IOException {
        m_model.loadBaseModel();
    }

    /**
     * Set previously serialized RNN state
     */
    public void loadSerializedState() throws Exception {
        m_model.loadSerializedState();
    }

    /**
     * Prime the forecaster with the input data
     *
     * @param batch the number of predictions to be made
     * @return prediction for each future time step
     * @throws Exception if a problem occurs
     */
    public void primeForecaster(Instances batch) throws Exception {
        m_model.primeForecaster(batch);
    }

    public List<String> getForecastDates(int stepsToForecast,
                                         Instance lastInst, int dateIndex) throws Exception {

        List<String> dates = new ArrayList<>(stepsToForecast);
        TSLagMaker tsLagMaker = m_model.getTSLagMaker();
        Attribute dateAtt = lastInst.attribute(dateIndex);
        double lastDate = tsLagMaker.getCurrentTimeStampValue();

        for (int i = 0; i < stepsToForecast; i++) {
            lastDate = tsLagMaker.advanceSuppliedTimeValue(lastDate);
            String date = dateAtt.formatDate(lastDate);
            dates.add(date);
        }
        return dates;
    }

    public void clearPreviousState() {
        m_model.clearPreviousState();
    }

    public void setPreviousState(List<Object> state) {
        m_model.setPreviousState(state);
    }

    public List<Object> getPreviousState() {
        return m_model.getPreviousState();
    }

    public List<String> getTargetFieldNames() {
        String[] strArr = m_model.getOptions();
        String[] targetStrs = strArr[1].split(",");
        List<String> targetFields = new ArrayList<>(targetStrs.length);
        for (int i = 0; i < targetStrs.length; i++) {
            targetFields.add(targetStrs[i]);
        }

        return targetFields;
    }

    /**
     * Return a classification (number for regression problems
     * or index of a class value for classification problems).
     *
     * @param numStepsToForecast number of steps to predict beyond training data
     * @param overlay overlay input data to be used if model was trained with overlay attributes
     * @return the predictions
     * @exception Exception if an error occurs
     */
    public List<List<NumericPrediction>> forecast(int numStepsToForecast, Instances overlay) throws Exception {
        return m_model.forecast(numStepsToForecast, overlay);
    }

    /**
     * Return a classification (number for regression problems
     * or index of a class value for classification problems).
     *
     * @param numStepsToForecast number of steps to predict beyond training data
     * @return the predictions
     * @exception Exception if an error occurs
     */
    public List<List<NumericPrediction>> forecast(int numStepsToForecast) throws Exception {
        return m_model.forecast(numStepsToForecast);
    }

    /**
     * Returns the textual description of the WekaForecaster's model.
     *
     * @return the WekaForecaster's model as a String
     */
    public String toString() {
        return m_model.toString();
    }
}
