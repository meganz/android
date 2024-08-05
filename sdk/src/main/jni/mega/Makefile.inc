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

CPP_SOURCES= sdk/src/attrmap.cpp \
    sdk/src/backofftimer.cpp \
    sdk/src/base64.cpp \
    sdk/src/command.cpp \
    sdk/src/commands.cpp \
    sdk/src/db.cpp \
    sdk/src/file.cpp \
    sdk/src/fileattributefetch.cpp \
    sdk/src/filefingerprint.cpp \
    sdk/src/filesystem.cpp \
    sdk/src/http.cpp \
    sdk/src/json.cpp \
    sdk/src/megaclient.cpp \
    sdk/src/node.cpp \
    sdk/src/pubkeyaction.cpp \
    sdk/src/request.cpp \
    sdk/src/serialize64.cpp \
    sdk/src/nodemanager.cpp \
    sdk/src/share.cpp \
    sdk/src/sharenodekeys.cpp \
    sdk/src/sync.cpp \
    sdk/src/syncfilter.cpp \
    sdk/src/transfer.cpp \
    sdk/src/transferslot.cpp \
    sdk/src/proxy.cpp \
    sdk/src/pendingcontactrequest.cpp \
    sdk/src/treeproc.cpp \
    sdk/src/user.cpp \
    sdk/src/useralerts.cpp \
    sdk/src/utils.cpp \
    sdk/src/waiterbase.cpp  \
    sdk/src/crypto/cryptopp.cpp  \
    sdk/src/crypto/sodium.cpp  \
    sdk/src/db/sqlite.cpp  \
    sdk/src/posix/net.cpp  \
    sdk/src/posix/fs.cpp  \
    sdk/src/posix/waiter.cpp \
    sdk/src/mega_utf8proc.cpp \
    sdk/src/gfx.cpp \
    sdk/src/gfx/external.cpp \
    sdk/src/thread/posixthread.cpp \
    sdk/src/logging.cpp \
    sdk/src/mega_http_parser.cpp \
    sdk/src/mega_zxcvbn.cpp \
    sdk/src/mediafileattribute.cpp \
    sdk/src/mega_evt_tls.cpp \
    sdk/src/mega_ccronexpr.cpp\
    sdk/src/raid.cpp \
    sdk/src/raidproxy.cpp \
    sdk/src/testhooks.cpp \
    sdk/src/heartbeats.cpp \
    sdk/src/textchat.cpp \
    sdk/src/setandelement.cpp \
    sdk/src/name_collision.cpp \
    sdk/src/pwm_file_parser.cpp

C_SOURCES = sdk/src/mega_glob.c

HEADERS= sdk/include/mega.h \
	    sdk/include/mega/account.h \
	    sdk/include/mega/attrmap.h \
	    sdk/include/mega/backofftimer.h \
	    sdk/include/mega/base64.h \
	    sdk/include/mega/command.h \
	    sdk/include/mega/console.h \
	    sdk/include/mega/syncfilter.h \
	    sdk/include/mega/db.h \
	    sdk/include/mega/file.h \
	    sdk/include/mega/fileattributefetch.h \
	    sdk/include/mega/filefingerprint.h \
	    sdk/include/mega/filesystem.h \
	    sdk/include/mega/http.h \
	    sdk/include/mega/json.h \
	    sdk/include/mega/megaapp.h \
	    sdk/include/mega/megaclient.h \
	    sdk/include/mega/node.h \
	    sdk/include/mega/pubkeyaction.h \
	    sdk/include/mega/request.h \
	    sdk/include/mega/serialize64.h \
	    sdk/include/mega/nodemanager.h \
	    sdk/include/mega/share.h \
	    sdk/include/mega/sharenodekeys.h \
	    sdk/include/mega/sync.h \
	    sdk/include/mega/transfer.h \
	    sdk/include/mega/transferslot.h \
	    sdk/include/mega/proxy.h \
	    sdk/include/mega/pendingcontactrequest.h \
	    sdk/include/mega/treeproc.h \
	    sdk/include/mega/types.h \
	    sdk/include/mega/user.h \
		sdk/include/mega/useralerts.h \
	    sdk/include/mega/utils.h \
	    sdk/include/mega/waiter.h \
	    sdk/include/mega/crypto/cryptopp.h  \
	    sdk/include/mega/db/sqlite.h  \
	    sdk/include/megaapi.h \
	    sdk/include/megaapi_impl.h \
	    sdk/include/mega/posix/meganet.h  \
		sdk/include/mega/posix/megasys.h  \
		sdk/include/mega/posix/megafs.h  \
		sdk/include/mega/posix/megawaiter.h \
		sdk/include/mega/config-android.h \
		sdk/include/mega/mega_glob.h \
		sdk/include/mega/gfx/external.h \
		sdk/include/mega/thread/posixthread.h \
		sdk/include/mega/logging.h \
	    sdk/include/mega/mega_http_parser.h \
	    sdk/include/mega/mega_zxcvbn.h \
		sdk/include/mega/mediafileattribute.h \
	    sdk/include/mega/mega_evt_tls.h \
	    sdk/include/mega/mega_evt_queue.h \
	    sdk/include/mega/raid.h \
        sdk/include/mega/raidproxy.h \
		sdk/include/mega/testhooks.h \
		sdk/include/mega/heartbeats.h \
		sdk/include/mega/textchat.h \
		sdk/include/mega/setandelement.h \
		sdk/include/mega/mega_csv.h \
		sdk/include/mega/name_collision.h \
		sdk/include/mega/pwm_file_parser.h

# FUSE (Common headers.)
HEADERS += \
    sdk/include/mega/fuse/common/fuse/common/activity_monitor.h \
    sdk/include/mega/fuse/common/fuse/common/activity_monitor_forward.h \
    sdk/include/mega/fuse/common/fuse/common/any_lock.h \
    sdk/include/mega/fuse/common/fuse/common/any_lock_forward.h \
    sdk/include/mega/fuse/common/fuse/common/any_lock_set.h \
    sdk/include/mega/fuse/common/fuse/common/any_lock_set_forward.h \
    sdk/include/mega/fuse/common/fuse/common/bind_handle.h \
    sdk/include/mega/fuse/common/fuse/common/bind_handle_forward.h \
    sdk/include/mega/fuse/common/fuse/common/client.h \
    sdk/include/mega/fuse/common/fuse/common/client_adapter.h \
    sdk/include/mega/fuse/common/fuse/common/client_callbacks.h \
    sdk/include/mega/fuse/common/fuse/common/client_forward.h \
    sdk/include/mega/fuse/common/fuse/common/database.h \
    sdk/include/mega/fuse/common/fuse/common/date_time.h \
    sdk/include/mega/fuse/common/fuse/common/date_time_forward.h \
    sdk/include/mega/fuse/common/fuse/common/error_or.h \
    sdk/include/mega/fuse/common/fuse/common/error_or_forward.h \
    sdk/include/mega/fuse/common/fuse/common/file_open_flag.h \
    sdk/include/mega/fuse/common/fuse/common/file_open_flag_forward.h \
    sdk/include/mega/fuse/common/fuse/common/inode_cache_flags.h \
    sdk/include/mega/fuse/common/fuse/common/inode_cache_flags_forward.h \
    sdk/include/mega/fuse/common/fuse/common/inode_id.h \
    sdk/include/mega/fuse/common/fuse/common/inode_id_forward.h \
    sdk/include/mega/fuse/common/fuse/common/inode_info.h \
    sdk/include/mega/fuse/common/fuse/common/inode_info_forward.h \
    sdk/include/mega/fuse/common/fuse/common/lock.h \
    sdk/include/mega/fuse/common/fuse/common/lock_forward.h \
    sdk/include/mega/fuse/common/fuse/common/lockable.h \
    sdk/include/mega/fuse/common/fuse/common/lockable_forward.h \
    sdk/include/mega/fuse/common/fuse/common/log_level.h \
    sdk/include/mega/fuse/common/fuse/common/log_level_forward.h \
    sdk/include/mega/fuse/common/fuse/common/logger.h \
    sdk/include/mega/fuse/common/fuse/common/logger_forward.h \
    sdk/include/mega/fuse/common/fuse/common/logging.h \
    sdk/include/mega/fuse/common/fuse/common/mount_event.h \
    sdk/include/mega/fuse/common/fuse/common/mount_event_forward.h \
    sdk/include/mega/fuse/common/fuse/common/mount_event_type.h \
    sdk/include/mega/fuse/common/fuse/common/mount_event_type_forward.h \
    sdk/include/mega/fuse/common/fuse/common/mount_flags.h \
    sdk/include/mega/fuse/common/fuse/common/mount_flags_forward.h \
    sdk/include/mega/fuse/common/fuse/common/mount_info.h \
    sdk/include/mega/fuse/common/fuse/common/mount_info_forward.h \
    sdk/include/mega/fuse/common/fuse/common/mount_inode_id.h \
    sdk/include/mega/fuse/common/fuse/common/mount_inode_id_forward.h \
    sdk/include/mega/fuse/common/fuse/common/mount_result.h \
    sdk/include/mega/fuse/common/fuse/common/mount_result_forward.h \
    sdk/include/mega/fuse/common/fuse/common/node_event.h \
    sdk/include/mega/fuse/common/fuse/common/node_event_forward.h \
    sdk/include/mega/fuse/common/fuse/common/node_event_observer.h \
    sdk/include/mega/fuse/common/fuse/common/node_event_observer_forward.h \
    sdk/include/mega/fuse/common/fuse/common/node_event_queue.h \
    sdk/include/mega/fuse/common/fuse/common/node_event_queue_forward.h \
    sdk/include/mega/fuse/common/fuse/common/node_event_type.h \
    sdk/include/mega/fuse/common/fuse/common/node_event_type_forward.h \
    sdk/include/mega/fuse/common/fuse/common/node_info.h \
    sdk/include/mega/fuse/common/fuse/common/node_info_forward.h \
    sdk/include/mega/fuse/common/fuse/common/normalized_path.h \
    sdk/include/mega/fuse/common/fuse/common/normalized_path_forward.h \
    sdk/include/mega/fuse/common/fuse/common/pending_callbacks.h \
    sdk/include/mega/fuse/common/fuse/common/query.h \
    sdk/include/mega/fuse/common/fuse/common/query_forward.h \
    sdk/include/mega/fuse/common/fuse/common/scoped_query.h \
    sdk/include/mega/fuse/common/fuse/common/scoped_query_forward.h \
    sdk/include/mega/fuse/common/fuse/common/service.h \
    sdk/include/mega/fuse/common/fuse/common/service_callbacks.h \
    sdk/include/mega/fuse/common/fuse/common/service_context.h \
    sdk/include/mega/fuse/common/fuse/common/service_context_forward.h \
    sdk/include/mega/fuse/common/fuse/common/service_flags.h \
    sdk/include/mega/fuse/common/fuse/common/service_flags_forward.h \
    sdk/include/mega/fuse/common/fuse/common/service_forward.h \
    sdk/include/mega/fuse/common/fuse/common/shared_mutex.h \
    sdk/include/mega/fuse/common/fuse/common/shared_mutex_forward.h \
    sdk/include/mega/fuse/common/fuse/common/task_executor.h \
    sdk/include/mega/fuse/common/fuse/common/task_executor_flags.h \
    sdk/include/mega/fuse/common/fuse/common/task_executor_flags_forward.h \
    sdk/include/mega/fuse/common/fuse/common/task_executor_forward.h \
    sdk/include/mega/fuse/common/fuse/common/task_queue.h \
    sdk/include/mega/fuse/common/fuse/common/task_queue_forward.h \
    sdk/include/mega/fuse/common/fuse/common/transaction.h \
    sdk/include/mega/fuse/common/fuse/common/transaction_forward.h \
    sdk/include/mega/fuse/common/fuse/common/type_traits.h \
    sdk/include/mega/fuse/common/fuse/common/upload.h \
    sdk/include/mega/fuse/common/fuse/common/upload_forward.h \
    sdk/include/mega/fuse/common/fuse/common/utility.h

# FUSE (Common sources.)
CPP_SOURCES += \
    sdk/src/fuse/common/activity_monitor.cpp \
    sdk/src/fuse/common/any_lock_set.cpp \
    sdk/src/fuse/common/bind_handle.cpp \
    sdk/src/fuse/common/client.cpp \
    sdk/src/fuse/common/client_adapter.cpp \
    sdk/src/fuse/common/client_adapter_with_sync.cpp \
    sdk/src/fuse/common/database.cpp \
    sdk/src/fuse/common/date_time.cpp \
    sdk/src/fuse/common/inode_id.cpp \
    sdk/src/fuse/common/inode_info.cpp \
    sdk/src/fuse/common/log_level.cpp \
    sdk/src/fuse/common/logger.cpp \
    sdk/src/fuse/common/mount_event.cpp \
    sdk/src/fuse/common/mount_event_type.cpp \
    sdk/src/fuse/common/mount_flags.cpp \
    sdk/src/fuse/common/mount_info.cpp \
    sdk/src/fuse/common/mount_inode_id.cpp \
    sdk/src/fuse/common/mount_result.cpp \
    sdk/src/fuse/common/node_event_type.cpp \
    sdk/src/fuse/common/normalized_path.cpp \
    sdk/src/fuse/common/pending_callbacks.cpp \
    sdk/src/fuse/common/query.cpp \
    sdk/src/fuse/common/scoped_query.cpp \
    sdk/src/fuse/common/service.cpp \
    sdk/src/fuse/common/service_context.cpp \
    sdk/src/fuse/common/shared_mutex.cpp \
    sdk/src/fuse/common/task_executor.cpp \
    sdk/src/fuse/common/task_queue.cpp \
    sdk/src/fuse/common/transaction.cpp \
    sdk/src/fuse/common/utility.cpp

# FUSE (Dummy headers.)
HEADERS += \
    sdk/src/fuse/unsupported/mega/fuse/platform/service_context.h

# FUSE (Dummy sources.)
CPP_SOURCES += \
    sdk/src/fuse/unsupported/service.cpp \
    sdk/src/fuse/unsupported/service_context.cpp

OBJECTS=$(CPP_SOURCES:.cpp=.o) $(C_SOURCES:.c=.o)
C_WRAPPER_SOURCES = sdk/src/megaapi.cpp sdk/src/megaapi_impl.cpp
C_WRAPPER_OBJECTS = sdk/src/megaapi.o sdk/src/megaapi_impl.o

PYTHON_WRAPS=python_wrap.cxx
PYTHON_HEADERS=python_wrap.h 
PYTHON_OBJECTS=python_wrap.o
PYTHON_OUTPUT_PATH=python/
PYTHON_BINDINGS=$(addprefix $(PYTHON_OUTPUT_PATH), mega.py)
PYTHON_LIBRARY=$(addprefix $(PYTHON_OUTPUT_PATH),_mega.so)


JAVA_PACKAGE_NAME=nz.mega.sdk
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

SWIG_INTERFACE=sdk/bindings/megaapi.i
