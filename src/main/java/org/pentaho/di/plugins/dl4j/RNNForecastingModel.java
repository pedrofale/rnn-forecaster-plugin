package org.pentaho.di.plugins.dl4j;

import java.io.Serializable;
import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Abstract wrapper class for a forecaster model. Provides a unified interface to
 * obtaining predictions. Subclasses (RNNForecastingClassifer) encapsulate the actual weka forecaster models.
 *
 * @author Pedro Ferreira (pferreira{[at]}pentaho{[dot]}org)
 * @version 1.0
 */
public abstract class RNNForecastingModel implements Serializable {

    // The header of the Instances used to build the model
    private Instances m_header;

    /**
     * Creates a new <code>RNNForecastingModel</code> instance.
     *
     * @param model the actual Weka model to enacpsulate
     */
    public RNNForecastingModel(Object model) {
        setModel(model);
    }

    /**
     * Set the Instances header
     *
     * @param header an <code>Instances</code> value
     */
    public void setHeader(Instances header) {
        m_header = header;
    }

    /**
     * Get the header of the Instances that was used build this Weka model
     *
     * @return an <code>Instances</code> value
     */
    public Instances getHeader() {
        return m_header;
    }

    /**
     * Tell the model that this forecasting run is finished.
     */
    public void done() {
        // subclasses override if they need to do
        // something here.
    }

    /**
     * Set the weka model
     *
     * @param model the Weka model
     */
    public abstract void setModel(Object model);

    /**
     * Get the weka model
     *
     * @return the Weka model as an object
     */
    public abstract Object getModel();

    /**
     * Set the base model
     */
    public abstract void loadBaseModel(String filename) throws Exception;

    /**
     * Set the serialized RNN state
     */
    public abstract void loadSerializedState(String filename) throws Exception;

    /**
     * Get the names of the forecasting targets
     */
    public abstract List<String> getTargetFieldNames();

    /**
     * Prime the forecaster with the input data
     *
     * @param batch the historical data needed by the forecaster
     * @throws Exception if a problem occurs
     */
    public abstract void primeForecaster(Instances batch) throws Exception;

    /**
     * Get the dates for the time steps to forecast
     *
     * @param stepsToForecast the number of predictions to be made
     * @return prediction for each future time step
     * @throws Exception if a problem occurs
     */
    public abstract List<String> getForecastDates(int stepsToForecast,
                                                         Instance lastInst, int dateIndex) throws Exception;

    public abstract void clearPreviousState();

    public abstract void setPreviousState(List<Object> state);

    public abstract List<Object> getPreviousState();

    /**
     * Forecasting method
     *
     * @param numOfSteps the number of predictions to be made
     * @param overlay overlay input data to be used if model was trained with overlay attributes
     * @return prediction for each future time step
     * @throws Exception if a problem occurs
     */
    public abstract List<List<NumericPrediction>> forecast(int numOfSteps, Instances overlay)
            throws Exception;


    /**
     * Return a classification (number for regression problems
     * or index of a class value for classification problems).
     *
     * @param numStepsToForecast number of steps to predict beyond training data
     * @return the predictions
     * @exception Exception if an error occurs
     */
    public abstract List<List<NumericPrediction>> forecast(int numStepsToForecast) throws Exception;

    /**
     * Static factory method to create an instance of an appropriate subclass of
     * RNNForecastingModel given a Weka model.
     *
     * @param model a forecasting Weka model
     * @return an appropriate RNNForecastingModel for this type of Weka model
     * @exception Exception if an error occurs
     */
    public static RNNForecastingModel createScorer(Object model) throws Exception {
        if (model instanceof WekaForecaster) {
            return new RNNForecastingClassifier(model);
        }
        return null;
    }
}

