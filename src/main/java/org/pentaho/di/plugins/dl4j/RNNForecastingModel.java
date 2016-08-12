package org.pentaho.di.plugins.dl4j;

/**
 * Created by pedro on 08-08-2016.
 */

import java.io.Serializable;
import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Instances;

/**
 * Abstract wrapper class for a Weka model. Provides a unified interface to
 * obtaining predictions. Subclasses ( RNNForecastingClassifer and
 * RNNForecastingClusterer) encapsulate the actual weka models.
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
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
     * Tell the model that this scoring run is finished.
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
     * Prime the forecaster with the input data
     *
     * @param batch the number of predictions to be made
     * @return prediction for each future time step
     * @throws Exception if a problem occurs
     */
    public abstract void primeForecaster(Instances batch)
            throws Exception;

    /**
     * Forecasting method
     *
     * @param numOfSteps the number of predictions to be made
     * @return prediction for each future time step
     * @throws Exception if a problem occurs
     */
    public abstract List<List<NumericPrediction>> forecast(int numOfSteps)
            throws Exception;

    /**
     * Static factory method to create an instance of an appropriate subclass of
     * RNNForecastingModel given a Weka model.
     *
     * @param model a Weka model
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

