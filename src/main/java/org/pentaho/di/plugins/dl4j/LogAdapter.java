package org.pentaho.di.plugins.dl4j;

/**
 * Created by pedro on 08-08-2016.
 */

import java.io.Serializable;

import org.pentaho.di.core.logging.LogChannelInterface;

import weka.gui.Logger;

/**
 * Adapts Kettle logging to Weka's Logger interface
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 * @version $Revision: 1.0 $
 */
public class LogAdapter implements Serializable, Logger {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 4861213857483800216L;

    private transient LogChannelInterface m_log;

    public LogAdapter(LogChannelInterface log) {
        m_log = log;
    }

    public void statusMessage(String message) {
        m_log.logDetailed(message);
    }

    public void logMessage(String message) {
        m_log.logBasic(message);
    }
}
