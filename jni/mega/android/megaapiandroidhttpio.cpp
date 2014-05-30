#include "megaapiandroidhttpio.h"

namespace mega {

void MegaApiCurlHttpIO::setProxy(MegaProxySettings *proxySettings)
{
}


void MegaApiCurlHttpIO::post(HttpReq* req, const char* data, unsigned len)
{
	if (debug)
	    {
	        cout << "POST target URL: " << req->posturl << endl;

	        if (req->binary)
	        {
	            cout << "[sending " << req->out->size() << " bytes of raw data]" << endl;
	        }
	        else
	        {
	            cout << "Sending: " << *req->out << endl;
	        }
	    }

	    CURL* curl;

	    req->in.clear();

	    if ((curl = curl_easy_init()))
	    {
	        curl_easy_setopt(curl, CURLOPT_URL, req->posturl.c_str());
	        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, data ? data : req->out->data());
	        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, data ? len : req->out->size());
	        curl_easy_setopt(curl, CURLOPT_USERAGENT, useragent->c_str());
	        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, req->type == REQ_JSON ? contenttypejson : contenttypebinary);
	        curl_easy_setopt(curl, CURLOPT_ENCODING, "");
	        curl_easy_setopt(curl, CURLOPT_SHARE, curlsh);
	        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
	        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void*)req);
	        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, check_header);
	        curl_easy_setopt(curl, CURLOPT_HEADERDATA, (void*)req);
	        curl_easy_setopt(curl, CURLOPT_PRIVATE, (void*)req);

	        //Verify SSL cert and host.
			curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1L);
			curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 2L);

			//Don't trust the default cacert bundle
			curl_easy_setopt(curl,CURLOPT_CAINFO, NULL);
			curl_easy_setopt(curl,CURLOPT_CAPATH, NULL);

			//Callback to load the the root certificate of the MEGA server.
			curl_easy_setopt(curl,CURLOPT_SSL_CTX_FUNCTION, MegaApiCurlHttpIO::sslctx_function);

	        curl_multi_add_handle(curlm, curl);

	        req->status = REQ_INFLIGHT;

	        req->httpiohandle = (void*)curl;
	    }
	    else
	    {
	        req->status = REQ_FAILURE;
	    }
}

MegaProxySettings *MegaApiCurlHttpIO::getAutoProxySettings()
{
    MegaProxySettings *proxySettings = new MegaProxySettings();
    proxySettings->setProxyType(MegaProxySettings::NONE);
    return proxySettings;
}

int MegaApiCurlHttpIO::ssl_verify_callback(X509_STORE_CTX *ctx, void *arg)
{
	//verify the certificate chain.
	int ok = ((X509_verify_cert(ctx)==1) && ctx->cert);
	if(!ok) return 0;

	//get an EVP_PKEY object with the MEGA public key.
	EVP_PKEY* evp = X509_PUBKEY_get(X509_get_X509_PUBKEY(ctx->cert));
	if(!evp) return 0;

	//modulus of the MEGA public key
	const unsigned char MEGAmodulus[] =
	{
			0xB6, 0x61, 0xE7, 0xCF, 0x69, 0x2A, 0x84, 0x35, 0x05, 0xC3, 0x14, 0xBC, 0x95, 0xCF, 0x94, 0x33,
			0x1C, 0x82, 0x67, 0x3B, 0x04, 0x35, 0x11, 0xA0, 0x8D, 0xC8, 0x9D, 0xBB, 0x9C, 0x79, 0x65, 0xE7,
			0x10, 0xD9, 0x91, 0x80, 0xC7, 0x81, 0x0C, 0xF4, 0x95, 0xBB, 0xB3, 0x26, 0x9B, 0x97, 0xD2, 0x14,
			0x0F, 0x0B, 0xCA, 0xF0, 0x5E, 0x45, 0x7B, 0x32, 0xC6, 0xA4, 0x7D, 0x7A, 0xFE, 0x11, 0xE7, 0xB2,
			0x5E, 0x21, 0x55, 0x23, 0x22, 0x1A, 0xCA, 0x1A, 0xF9, 0x21, 0xE1, 0x4E, 0xB7, 0x82, 0x0D, 0xEB,
			0x9D, 0xCB, 0x4E, 0x3D, 0x0B, 0xE4, 0xED, 0x4A, 0xEF, 0xE4, 0xAB, 0x0C, 0xEC, 0x09, 0x69, 0xFE,
			0xAE, 0x43, 0xEC, 0x19, 0x04, 0x3D, 0x5B, 0x68, 0x0F, 0x67, 0xE8, 0x80, 0xFF, 0x9B, 0x03, 0xEA,
			0x50, 0xAB, 0x16, 0xD7, 0xE0, 0x4C, 0xB4, 0x42, 0xEF, 0x31, 0xE2, 0x32, 0x9F, 0xE4, 0xD5, 0xF4,
			0xD8, 0xFD, 0x82, 0xCC, 0xC4, 0x50, 0xD9, 0x4D, 0xB5, 0xFB, 0x6D, 0xA2, 0xF3, 0xAF, 0x37, 0x67,
			0x7F, 0x96, 0x4C, 0x54, 0x3D, 0x9B, 0x1C, 0xBD, 0x5C, 0x31, 0x6D, 0x10, 0x43, 0xD8, 0x22, 0x21,
			0x01, 0x87, 0x63, 0x22, 0x89, 0x17, 0xCA, 0x92, 0xCB, 0xCB, 0xEC, 0xE8, 0xC7, 0xFF, 0x58, 0xE8,
			0x18, 0xC4, 0xCE, 0x1B, 0xE5, 0x4F, 0x20, 0xA8, 0xCF, 0xD3, 0xB9, 0x9D, 0x5A, 0x7A, 0x69, 0xF2,
			0xCA, 0x48, 0xF8, 0x87, 0x95, 0x3A, 0x32, 0x70, 0xB3, 0x1A, 0xF0, 0xC4, 0x45, 0x70, 0x43, 0x58,
			0x18, 0xDA, 0x85, 0x29, 0x1D, 0xAF, 0x83, 0xC2, 0x35, 0xA9, 0xC1, 0x73, 0x76, 0xB4, 0x47, 0x22,
			0x2B, 0x42, 0x9F, 0x93, 0x72, 0x3F, 0x9D, 0x3D, 0xA1, 0x47, 0x3D, 0xB0, 0x46, 0x37, 0x1B, 0xFD,
			0x0E, 0x28, 0x68, 0xA0, 0xF6, 0x1D, 0x62, 0xB2, 0xDC, 0x69, 0xC7, 0x9B, 0x09, 0x1E, 0xB5, 0x47
	};

	//exponent of the MEGA public key
	const unsigned char MEGAexponent[] = {0x01, 0x00, 0x01};

	//check the length of the modulus
	int len = BN_num_bytes(evp->pkey.rsa->n);
	if(len != sizeof(MEGAmodulus))
	{
		EVP_PKEY_free(evp);
		return 0;
	}

	//convert the public modulus to a binary string
	unsigned char *buff = new unsigned char[len];
	BN_bn2bin(evp->pkey.rsa->n, buff);

	//check the public key
	ok = (memcmp(buff, MEGAmodulus, (size_t)len) == 0);
	delete buff;

	//check the length of the exponent
	len = BN_num_bytes(evp->pkey.rsa->e);
	if(len != sizeof(MEGAexponent))
	{
		EVP_PKEY_free(evp);
		return 0;
	}

	//convert the public exponent to a binary string
	buff = new unsigned char[len];
	BN_bn2bin(evp->pkey.rsa->e, buff);

	//check the public exponent
	ok &= (memcmp(buff, MEGAexponent, (size_t)len) == 0);
	delete buff;

	EVP_PKEY_free(evp);
	return ok;
}

CURLcode MegaApiCurlHttpIO::sslctx_function(CURL * curl, void * sslctx, void * parm)
{
	X509_STORE * store;
	X509 * cert=NULL;
	BIO * bio;

	//CA in the root of the MEGA certificate.
	const char *MEGApemRoot =
			"UTN DATACorp SGC Root CA\n"
			"========================\n"
			"-----BEGIN CERTIFICATE-----\n"
			"MIIEXjCCA0agAwIBAgIQRL4Mi1AAIbQR0ypoBqmtaTANBgkqhkiG9w0BAQUFADCBkzELMAkGA1UE\n"
			"BhMCVVMxCzAJBgNVBAgTAlVUMRcwFQYDVQQHEw5TYWx0IExha2UgQ2l0eTEeMBwGA1UEChMVVGhl\n"
			"IFVTRVJUUlVTVCBOZXR3b3JrMSEwHwYDVQQLExhodHRwOi8vd3d3LnVzZXJ0cnVzdC5jb20xGzAZ\n"
			"BgNVBAMTElVUTiAtIERBVEFDb3JwIFNHQzAeFw05OTA2MjQxODU3MjFaFw0xOTA2MjQxOTA2MzBa\n"
			"MIGTMQswCQYDVQQGEwJVUzELMAkGA1UECBMCVVQxFzAVBgNVBAcTDlNhbHQgTGFrZSBDaXR5MR4w\n"
			"HAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxITAfBgNVBAsTGGh0dHA6Ly93d3cudXNlcnRy\n"
			"dXN0LmNvbTEbMBkGA1UEAxMSVVROIC0gREFUQUNvcnAgU0dDMIIBIjANBgkqhkiG9w0BAQEFAAOC\n"
			"AQ8AMIIBCgKCAQEA3+5YEKIrblXEjr8uRgnn4AgPLit6E5Qbvfa2gI5lBZMAHryv4g+OGQ0SR+ys\n"
			"raP6LnD43m77VkIVni5c7yPeIbkFdicZD0/Ww5y0vpQZY/KmEQrrU0icvvIpOxboGqBMpsn0GFlo\n"
			"wHDyUwDAXlCCpVZvNvlK4ESGoE1O1kduSUrLZ9emxAW5jh70/P/N5zbgnAVssjMiFdC04MwXwLLA\n"
			"9P4yPykqlXvY8qdOD1R8oQ2AswkDwf9c3V6aPryuvEeKaq5xyh+xKrhfQgUL7EYw0XILyulWbfXv\n"
			"33i+Ybqypa4ETLyorGkVl73v67SMvzX41MPRKA5cOp9wGDMgd8SirwIDAQABo4GrMIGoMAsGA1Ud\n"
			"DwQEAwIBxjAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBRTMtGzz3/64PGgXYVOktKeRR20TzA9\n"
			"BgNVHR8ENjA0MDKgMKAuhixodHRwOi8vY3JsLnVzZXJ0cnVzdC5jb20vVVROLURBVEFDb3JwU0dD\n"
			"LmNybDAqBgNVHSUEIzAhBggrBgEFBQcDAQYKKwYBBAGCNwoDAwYJYIZIAYb4QgQBMA0GCSqGSIb3\n"
			"DQEBBQUAA4IBAQAnNZcAiosovcYzMB4p/OL31ZjUQLtgyr+rFywJNn9Q+kHcrpY6CiM+iVnJowft\n"
			"Gzet/Hy+UUla3joKVAgWRcKZsYfNjGjgaQPpxE6YsjuMFrMOoAyYUJuTqXAJyCyjj98C5OBxOvG0\n"
			"I3KgqgHf35g+FFCgMSa9KOlaMCZ1+XtgHI3zzVAmbQQnmt/VDUVHKWss5nbZqSl9Mt3JNjy9rjXx\n"
			"EZ4du5A/EkdOjtd+D2JzHVImOBwYSf0wdJrE5SIv2MCN7ZF6TACPcn9d2t0bi0Vr591pl6jFVkwP\n"
			"DPafepE39peC4N1xaf92P2BNPM/3mfnGV/TJVTl4uix5yaaIK/QI\n"
			"-----END CERTIFICATE-----\n";

	SSL_CTX *ctx = (SSL_CTX *)sslctx;

	/* get a BIO */
	bio=BIO_new_mem_buf((char *)MEGApemRoot, -1);

	/* use it to read the PEM formatted certificate from memory into an X509
	 * structure that SSL can use*/
	PEM_read_bio_X509(bio, &cert, 0, NULL);
	(void)BIO_set_close(bio, BIO_NOCLOSE);
	BIO_free(bio);

	if(cert == NULL) return CURLE_SSL_CACERT_BADFILE;

	/* get a pointer to the X509 certificate store (which may be empty!) */
	store=SSL_CTX_get_cert_store(ctx);
	if (store == NULL) { X509_free(cert); return CURLE_SSL_CACERT_BADFILE; }

	/* add the certificate to this store */
	X509_STORE_add_cert(store, cert);
	X509_free(cert);

	/* set max depth for the certificate chain */
	SSL_CTX_set_verify_depth(ctx, 3);

	/* set a custom callback to check the certificate */
	SSL_CTX_set_cert_verify_callback(ctx, MegaApiCurlHttpIO::ssl_verify_callback, NULL);

	/* all set to go */
	return CURLE_OK ;
}


}
