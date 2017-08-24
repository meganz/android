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

import javax.swing.SwingUtilities;

/**
 * Control a MEGA account or a shared folder using a Java Swing GUI.
 *
 * @see MegaApiJava
 */
public class MegaApiSwing extends MegaApiJava {

    /**
     * Instantiates a new MEGA API using Swing.
     *
     * @param appKey
     *              AppKey of your application. You can generate your AppKey for free here: <br>
     *              https://mega.co.nz/#sdk.
     * @param userAgent
     *              User agent to use in network requests. If you pass null to this parameter,
     *              a default user agent will be used.
     * @param path
     *              Base path to store the local cache. If you pass null to this parameter,
     *              the SDK will not use any local cache.
     * @see MegaApiJava#MegaApiJava(String appKey, String userAgent, String basePath, MegaGfxProcessor gfxProcessor)
     */
    public MegaApiSwing(String appKey, String userAgent, String path) {
        super(appKey, userAgent, path, new MegaGfxProcessor());
    }

    @Override
    void runCallback(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }
}
