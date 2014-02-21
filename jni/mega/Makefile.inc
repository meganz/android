JAVA_BASE_OUTPUT_PATH?=java

CXX=g++
SWIG=swig
JAVAC=javac

CPPFLAGS=-Wall -fPIC -g -isystem /usr/include/cryptopp -I /usr/local/include/ -I . -I include

CFLAGS_JAVA_LIBRARY= -I /usr/lib/jvm/java/include/ -I /usr/lib/jvm/java/include/linux
CFLAGS_PYTHON_LIBRARY= -I /usr/include/python2.7
CFLAGS_ANDROID_LIBRARY=

LIB=-lpthread -lcryptopp -lcurl
LIB_JAVA_LIBRARY= -lcryptopp -lcurl
LIB_PYTHON_LIBRARY= -lcryptopp -lcurl -lpython2.7
LIB_ANDROID_LIBRARY= -lcryptopp -lcurl

CPP_SOURCES= src/attrmap.cpp \
    src/backofftimer.cpp \
    src/base64.cpp \
    src/command.cpp \
    src/commands.cpp \
    src/db.cpp \
    src/file.cpp \
    src/fileattributefetch.cpp \
    src/filefingerprint.cpp \
    src/filesystem.cpp \
    src/http.cpp \
    src/json.cpp \
    src/megaclient.cpp \
    src/node.cpp \
    src/pubkeyaction.cpp \
    src/request.cpp \
    src/serialize64.cpp \
    src/share.cpp \
    src/sharenodekeys.cpp \
    src/sync.cpp \
    src/transfer.cpp \
    src/transferslot.cpp \
    src/treeproc.cpp \
    src/user.cpp \
    src/utils.cpp \
    src/waiter.cpp  \
    src/crypto/cryptopp.cpp  \
    src/db/sqlite.cpp  \
    MegaProxySettings.cpp \
    src/posix/net.cpp  \
    src/posix/fs.cpp  \
    src/posix/posixwaiter.cpp \
    linux/megaapiwait.cpp \
    android/megaapiandroidhttpio.cpp
    
C_SOURCES = sqlite3.c android/glob.c

HEADERS= include/mega.h \
	    include/mega/account.h \
	    include/mega/attrmap.h \
	    include/mega/backofftimer.h \
	    include/mega/base64.h \
	    include/mega/command.h \
	    include/mega/console.h \
	    include/mega/db.h \
	    include/mega/file.h \
	    include/mega/fileattributefetch.h \
	    include/mega/filefingerprint.h \
	    include/mega/filesystem.h \
	    include/mega/http.h \
	    include/mega/json.h \
	    include/mega/megaapp.h \
	    include/mega/megaclient.h \
	    include/mega/node.h \
	    include/mega/pubkeyaction.h \
	    include/mega/request.h \
	    include/mega/serialize64.h \
	    include/mega/share.h \
	    include/mega/sharenodekeys.h \
	    include/mega/sync.h \
	    include/mega/transfer.h \
	    include/mega/transferslot.h \
	    include/mega/treeproc.h \
	    include/mega/types.h \
	    include/mega/user.h \
	    include/mega/utils.h \
	    include/mega/waiter.h \
	    include/mega/crypto/cryptopp.h  \
	    include/mega/db/sqlite.h  \
	    megaapi.h \
	    MegaProxySettings.h \
	    include/mega/posix/meganet.h  \
        include/mega/posix/megasys.h  \
        include/mega/posix/megafs.h  \
        include/mega/posix/megawaiter.h \
        linux/megaapiwait.h  \
        include/mega/config.h \
        android/glob.h

OBJECTS=$(CPP_SOURCES:.cpp=.o) $(C_SOURCES:.c=.o)
C_WRAPPER_SOURCES = megaapi.cpp
C_WRAPPER_OBJECTS = megaapi.o

PYTHON_WRAPS=python_wrap.cxx
PYTHON_HEADERS=python_wrap.h 
PYTHON_OBJECTS=python_wrap.o
PYTHON_OUTPUT_PATH=python/
PYTHON_BINDINGS=$(addprefix $(PYTHON_OUTPUT_PATH), mega.py)
PYTHON_LIBRARY=$(addprefix $(PYTHON_OUTPUT_PATH),_mega.so)


JAVA_PACKAGE_NAME=com.mega.sdk
JAVA_OUTPUT_PATH=$(JAVA_BASE_OUTPUT_PATH)/$(subst .,/,$(JAVA_PACKAGE_NAME))
JAVA_LIBRARY_OUTPUT_PATH=java
JAVA_LIBRARY=$(addprefix $(JAVA_LIBRARY_OUTPUT_PATH)/,libmega.so)

JAVA_WRAPS=java_wrap.cxx 
JAVA_HEADERS=java_wrap.h  
JAVA_OBJECTS=java_wrap.o

JAVA_BINDINGS=$(addprefix $(JAVA_OUTPUT_PATH)/, \
				AccountBalance.java BalanceList.java MegaApi.java MegaGlobalListener.java \
				MegaRequestListener.java NodeList.java ShareList.java UserList.java \
				AccountDetails.java mega.java MegaTransfer.java StringList.java \
				AccountPurchase.java megaJNI.java MegaTransferListener.java  PurchaseList.java \
				TransactionList.java AccountSession.java MegaListener.java SessionList.java \
				TreeProcessor.java AccountTransaction.java MegaError.java MegaRequest.java \
				Node.java Share.java User.java)

SWIG_INTERFACE=megaapi.i
