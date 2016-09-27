/*
 * (c) 2013-2015 by Mega Limited, Auckland, New Zealand
 *
 * This file is part of the MEGA SDK - Client Access Engine.
 *
 * Applications using the MEGA API must present a valid application key
 * and comply with the the rules set forth in the Terms of Service.
 *
 * The MEGA SDK is distributed in the hope that it will be useful,\
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * @copyright Simplified (2-clause) BSD License.
 * You should have received a copy of the license along with this
 * program.
 */
package nz.mega.sdk;

import java.lang.String;

/**
 * Interface to receive SDK logs.
 * You can implement this class and pass an object of your subclass to MegaApiJava.setLoggerClass()
 * to receive SDK logs. You will also have to use MegaApiJava.setLogLevel() to select the level of
 * the logs that you want to receive.
 *
 * @see MegaChatLoggerInterface
 * @see MegaChatLogger
 */
class DelegateMegaChatLogger extends MegaChatLogger {
    MegaChatLoggerInterface listener;

    DelegateMegaChatLogger(MegaChatLoggerInterface listener) {
        this.listener = listener;
    }

    /**
     * This function will be called with all logs with level <= your selected level of logging.
     * By default logging level is MegaApi.LOG_LEVEL_INFO.
     *
     * @param loglevel
     *            Log level of this message.
     *            Valid values are: <br>
     *          - LOG_LEVEL_FATAL = 0. <br>
     *          - LOG_LEVEL_ERROR = 1. <br>
     *          - LOG_LEVEL_WARNING = 2. <br>
     *          - LOG_LEVEL_INFO = 3. <br>
     *          - LOG_LEVEL_DEBUG = 4. <br>
     *          - LOG_LEVEL_MAX = 5.
     * @param message
     *            Log message.
     *            The SDK retains the ownership of this string, it will be valid after this function returns.
     * @see MegaChatLoggerInterface#log(int loglevel, String message)
     * @see MegaChatLogger#log(int loglevel, String message)
     */
    public void log(int loglevel, String message) {
        if (listener != null) {
            listener.log(loglevel, message);
        }
    }
}
