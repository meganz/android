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

/**
 * Interface to receive information about requests.
 * <p>
 * All requests are able to pass a pointer to an implementation of this interface in the last parameter.
 * You can also get information about all requests using MegaApi.addRequestListener().
 * MegaListener objects can also receive information about requests.
 * This interface uses MegaRequest objects to provide information of requests. Take into account that not all fields
 * of MegaRequest objects are valid for all requests. See the documentation about each request to know which fields
 * contain useful information for each one.
 */
public interface MegaRequestListenerInterface {
    /**
     * This function is called when a request is about to start being processed.
     * <P>
     * The SDK retains the ownership of the request parameter.
     * Don't use it after this function returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *
     * @param api
     *              API that started the request.
     * @param request
     *              Information about the request.
     */
    public void onRequestStart(MegaApiJava api, MegaRequest request);

    /**
     * This function is called to inform about the progress of a request.
     * <p>
     * Currently, this callback is only used for fetchNodes requests (MegaRequest.TYPE_FETCH_NODES).
     * The SDK retains the ownership of the request parameter. Do not use it after this function returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *
     * @param api
     *            API that started the request.
     * @param request
     *            Information about the request.
     */
    public void onRequestUpdate(MegaApiJava api, MegaRequest request);

    /**
     * This function is called when a request has finished.
     * <p>
     * There will be no more callbacks about this request. The last parameter provides the result of the request.
     * If the request finished without problems, the error code will be API_OK. The SDK retains ownership of the
     * request and error parameters. Do not use them after this functions returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *  
     * @param api
     *            API that started the request.
     * @param request
     *            Information about the request.
     * @param e
     *            Error Information.
     */
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e);

    /**
     * This function is called when there is a temporary error processing a request.
     * <p>
     * The request continues after this callback, so expect more MegaRequestListener.onRequestTemporaryError
     * or a MegaRequestListener.onRequestFinish callback. The SDK retains the ownership of the request and
     * error parameters. Do not use them after this function returns.
     * The api object is the one created by the application, it will be valid until the application deletes it.
     *  
     * @param api
     *            API that started the request.
     * @param request
     *            Information about the request.
     * @param e
     *            Error Information.
     */
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e);
}
