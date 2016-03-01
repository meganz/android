include Application.mk

##################################################
### SET THE PATH TO YOUR ANDROID NDK DIRECTORY ###
##################################################
NDK_ROOT=$(HOME)/android-ndk
##################################################

NDK_BUILD=$(NDK_ROOT)/ndk-build
JNI_PATH=$(shell pwd)
CC=$(shell $(NDK_ROOT)/ndk-which gcc)
LIBDIR=$(JNI_PATH)/../obj/local/armeabi

CRYPTOPP=cryptopp
CRYPTOPP_VERSION=562
CRYPTOPP_SOURCE_FILE=cryptopp$(CRYPTOPP_VERSION).zip
CRYPTOPP_SOURCE_FOLDER=$(CRYPTOPP)/$(CRYPTOPP)
CRYPTOPP_DOWNLOAD_URL=http://www.cryptopp.com/$(CRYPTOPP_SOURCE_FILE)
CRYPTOPP_VERIFIED_SOURCES=$(CRYPTOPP)/$(CRYPTOPP_SOURCE_FILE)_verified

SQLITE=sqlite
SQLITE_VERSION=3080702
SQLITE_YEAR=2014
SQLITE_BASE_NAME=sqlite-amalgamation-$(SQLITE_VERSION)
SQLITE_SOURCE_FILE=$(SQLITE_BASE_NAME).zip
SQLITE_SOURCE_FOLDER=$(SQLITE)/$(SQLITE)
SQLITE_DOWNLOAD_URL=http://www.sqlite.org/$(SQLITE_YEAR)/$(SQLITE_SOURCE_FILE)
SQLITE_VERIFIED_SOURCES=$(SQLITE)/$(SQLITE_SOURCE_FILE)_verified

CURL=curl
CURL_VERSION=7.45.0
C_ARES_VERSION=1.10.0
CURL_EXTRA=--disable-ftp --disable-file --disable-ldap --disable-ldaps --disable-rtsp --disable-proxy --disable-dict --disable-telnet --disable-tftp --disable-pop3 --disable-imap --disable-smtp --disable-gopher --disable-sspi
CURL_SOURCE_FILE=curl-$(CURL_VERSION).tar.gz
CURL_SOURCE_FOLDER=curl-$(CURL_VERSION)
CURL_CONFIGURED=$(CURL)/$(CURL_SOURCE_FOLDER)/curl-config
CURL_DOWNLOAD_URL=http://curl.haxx.se/download/$(CURL_SOURCE_FILE)
CURL_VERIFIED_SOURCES=$(CURL)/$(CURL_SOURCE_FILE)_verified

ARES_SOURCE_FILE=c-ares-$(C_ARES_VERSION).tar.gz
ARES_SOURCE_FOLDER=c-ares-$(C_ARES_VERSION)
ARES_CONFIGURED=$(CURL)/$(ARES_SOURCE_FOLDER)/Makefile.inc
ARES_DOWNLOAD_URL=http://c-ares.haxx.se/download/$(ARES_SOURCE_FILE)
ARES_VERIFIED_SOURCES=$(CURL)/$(ARES_SOURCE_FILE)_verified

OPENSSL=openssl
OPENSSL_VERSION=1.0.1j
OPENSSL_SOURCE_FILE=openssl-$(OPENSSL_VERSION).tar.gz
OPENSSL_SOURCE_FOLDER=$(OPENSSL)-$(OPENSSL_VERSION)
OPENSSL_DOWNLOAD_URL=http://www.openssl.org/source/$(OPENSSL_SOURCE_FILE)
OPENSSL_VERIFIED_SOURCES=$(OPENSSL)/$(OPENSSL_SOURCE_FILE)_verified
OPENSSL_CONFIGURED=openssl/openssl/lib/libcrypto.a
OPENSSL_PREFIX=$(JNI_PATH)/$(OPENSSL)/$(OPENSSL_SOURCE_FOLDER)

SODIUM=sodium
SODIUM_VERSION=1.0.1
SODIUM_SOURCE_FILE=libsodium-$(SODIUM_VERSION).tar.gz
SODIUM_SOURCE_FOLDER=libsodium-$(SODIUM_VERSION)
SODIUM_DOWNLOAD_URL=https://download.libsodium.org/libsodium/releases/$(SODIUM_SOURCE_FILE)
SODIUM_VERIFIED_SOURCES=$(SODIUM)/$(SODIUM_SOURCE_FILE)_verified
SODIUM_BUILT=$(SODIUM)/$(SODIUM_SOURCE_FILE)_built

MEGA=mega
MEGA_JAVA_OUTPUT_PATH=$(JNI_PATH)/../src
MEGA_CONFIGURED=java_wrap.cxx
MEGA_WRAPPER=sdk/bindings/megaapi.i

all: $(CRYPTOPP) $(CURL) $(SQLITE) $(MEGA)
	$(NDK_BUILD)

$(OPENSSL): $(OPENSSL_CONFIGURED)

$(OPENSSL_CONFIGURED): $(OPENSSL_VERIFIED_SOURCES)
	cd $(OPENSSL)/; tar --overwrite -xf $(OPENSSL_SOURCE_FILE)
	ln -sf $(OPENSSL_SOURCE_FOLDER) $(OPENSSL)/$(OPENSSL)
	ln -sf $(LIBDIR) $(OPENSSL)/$(OPENSSL_SOURCE_FOLDER)/lib
	-$(NDK_BUILD) -k ssl crypto
	
$(OPENSSL_VERIFIED_SOURCES):
	wget -O $(OPENSSL)/$(OPENSSL_SOURCE_FILE) $(OPENSSL_DOWNLOAD_URL)
	sha1sum -c $(OPENSSL)/$(OPENSSL_SOURCE_FILE).sha1
	touch $(OPENSSL_VERIFIED_SOURCES)

$(CURL): $(CURL_CONFIGURED)
	
$(CURL_CONFIGURED): $(CURL_VERIFIED_SOURCES) $(ARES_VERIFIED_SOURCES) $(OPENSSL)
	cd $(CURL)/; tar --overwrite -xf $(CURL_SOURCE_FILE)
	cd $(CURL)/; tar --overwrite -xf $(ARES_SOURCE_FILE)
	ln -sf ../$(ARES_SOURCE_FOLDER) $(CURL)/$(CURL_SOURCE_FOLDER)/ares
	ln -sf $(CURL_SOURCE_FOLDER) $(CURL)/$(CURL)
	ln -sf $(ARES_SOURCE_FOLDER) $(CURL)/ares
	cd $(CURL)/$(CURL_SOURCE_FOLDER); ./configure CC=$(CC) --host=arm-linux \
		CPPFLAGS="-DANDROID " CFLAGS="--sysroot=$(NDK_ROOT)/platforms/$(APP_PLATFORM)/arch-arm" \
		LIBS="-lc -ldl -lz" \
		--enable-ipv6 --disable-manual --with-random=/dev/urandom \
		--with-ssl=$(OPENSSL_PREFIX) --without-ca-bundle --without-ca-path \
		--with-zlib --enable-ares $(CURL_EXTRA)

$(ARES_VERIFIED_SOURCES):	
	wget -O $(CURL)/$(ARES_SOURCE_FILE) $(ARES_DOWNLOAD_URL)
	cd $(CURL); sha1sum -c $(ARES_SOURCE_FILE).sha1
	touch $(ARES_VERIFIED_SOURCES)

$(CURL_VERIFIED_SOURCES):
	wget -O $(CURL)/$(CURL_SOURCE_FILE) $(CURL_DOWNLOAD_URL)
	cd $(CURL); sha1sum -c $(CURL_SOURCE_FILE).sha1
	touch $(CURL_VERIFIED_SOURCES)

$(MEGA): $(MEGA)/$(MEGA_CONFIGURED)

$(MEGA)/$(MEGA_CONFIGURED): $(MEGA)/$(MEGA_WRAPPER)
	make clean_mega
	make -C $(MEGA) -f MakefileBindings $(MEGA_CONFIGURED) JAVA_BASE_OUTPUT_PATH=$(MEGA_JAVA_OUTPUT_PATH)

$(CRYPTOPP): $(CRYPTOPP_SOURCE_FOLDER)

$(CRYPTOPP_SOURCE_FOLDER): $(CRYPTOPP_VERIFIED_SOURCES)
	unzip $(CRYPTOPP)/$(CRYPTOPP_SOURCE_FILE) -d $(CRYPTOPP_SOURCE_FOLDER)
	patch $(CRYPTOPP)/cryptopp/wait.h < $(CRYPTOPP)/include.patch

$(CRYPTOPP_VERIFIED_SOURCES):
	wget -O $(CRYPTOPP)/$(CRYPTOPP_SOURCE_FILE) $(CRYPTOPP_DOWNLOAD_URL)
	cd $(CRYPTOPP); sha1sum -c $(CRYPTOPP_SOURCE_FILE).sha1
	touch $(CRYPTOPP_VERIFIED_SOURCES)


$(SQLITE): $(SQLITE_SOURCE_FOLDER)

$(SQLITE_SOURCE_FOLDER): $(SQLITE_VERIFIED_SOURCES)
	unzip -o $(SQLITE)/$(SQLITE_SOURCE_FILE) -d $(SQLITE)
	ln -fs $(SQLITE_BASE_NAME) $(SQLITE_SOURCE_FOLDER)

$(SQLITE_VERIFIED_SOURCES):
	wget -O $(SQLITE)/$(SQLITE_SOURCE_FILE) $(SQLITE_DOWNLOAD_URL)
	cd $(SQLITE); sha1sum -c $(SQLITE_SOURCE_FILE).sha1
	touch $(SQLITE_VERIFIED_SOURCES)

$(SODIUM): $(SODIUM_BUILT)

$(SODIUM_BUILT): $(SODIUM_VERIFIED_SOURCES)
	cd $(SODIUM)/; tar --overwrite -xf $(SODIUM_SOURCE_FILE)
	ln -sf $(SODIUM_SOURCE_FOLDER) $(SODIUM)/$(SODIUM)
	cd $(SODIUM)/$(SODIUM) && ./autogen.sh
	cd $(SODIUM)/$(SODIUM) && export ANDROID_NDK_HOME=$(NDK_ROOT) && dist-build/android-x86.sh
	cd $(SODIUM)/$(SODIUM) && export ANDROID_NDK_HOME=$(NDK_ROOT) && dist-build/android-arm.sh
	ln -sf libsodium-android-arm $(SODIUM)/$(SODIUM)/libsodium-android-armeabi
	ln -sf libsodium-android-arm $(SODIUM)/$(SODIUM)/libsodium-android-armeabi-v7
	touch $(SODIUM_BUILT)

$(SODIUM_VERIFIED_SOURCES):
	wget -O $(SODIUM)/$(SODIUM_SOURCE_FILE) $(SODIUM_DOWNLOAD_URL)
	cd $(SODIUM); sha1sum -c $(SODIUM_SOURCE_FILE).sha1
	touch $(SODIUM_VERIFIED_SOURCES)

clean_mega:
	-make -C $(MEGA) -f MakefileBindings clean JAVA_BASE_OUTPUT_PATH=$(MEGA_JAVA_OUTPUT_PATH)

clean: clean_mega
	rm -rf $(CRYPTOPP_SOURCE_FOLDER)
	rm -rf $(SQLITE_SOURCE_FOLDER) $(SQLITE)/$(SQLITE_BASE_NAME)
	rm -rf $(CURL)/$(CURL_SOURCE_FOLDER)
	rm -rf $(CURL)/$(CURL)	
	rm -rf $(CURL)/$(ARES_SOURCE_FOLDER)
	rm -rf $(CURL)/ares
	rm -rf $(OPENSSL)/$(OPENSSL_SOURCE_FOLDER)
	rm -rf $(OPENSSL)/$(OPENSSL)
	rm -rf $(SODIUM)/$(SODIUM_SOURCE_FOLDER)
	rm -rf $(SODIUM)/$(SODIUM)
	rm -rf $(SODIUM_BUILT)
	rm -rf ../obj/local/armeabi/
	rm -rf ../obj/local/x86

veryclean: clean
	rm -rf $(CRYPTOPP)/$(CRYPTOPP_SOURCE_FILE) $(CRYPTOPP_VERIFIED_SOURCES)
	rm -rf $(SQLITE)/$(SQLITE_SOURCE_FILE) $(SQLITE_VERIFIED_SOURCES)
	rm -rf $(CURL)/$(CURL_SOURCE_FILE) $(CURL_VERIFIED_SOURCES)
	rm -rf $(CURL)/$(ARES_SOURCE_FILE) $(ARES_VERIFIED_SOURCES)
	rm -rf $(OPENSSL)/$(OPENSSL_SOURCE_FILE) $(OPENSSL_VERIFIED_SOURCES)
	rm -rf $(SODIUM)/$(SODIUM_SOURCE_FILE) $(SODIUM_VERIFIED_SOURCES)

