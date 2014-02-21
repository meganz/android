#ifndef MEGAAPIANDROIDHTTPIO_H
#define MEGAAPIANDROIDHTTPIO_H

#include <openssl/ssl.h>
#include "mega/posix/meganet.h"
#include "MegaProxySettings.h"

namespace mega {

class MegaApiCurlHttpIO : public CurlHttpIO
{
protected:
    string proxyUsername;
    string proxyPassword;

	static CURLcode sslctx_function(CURL * curl, void * sslctx, void * parm);
	static int ssl_verify_callback(X509_STORE_CTX *ctx, void *arg);

public:
    void setProxy(MegaProxySettings *proxySettings);
    MegaProxySettings *getAutoProxySettings();
    void post(HttpReq* req, const char* data, unsigned len);
};


}

#endif // MEGAAPIANDROIDHTTPIO_H
