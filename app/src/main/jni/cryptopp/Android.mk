LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := cryptopp

LOCAL_CFLAGS := -DCRYPTOPP_DISABLE_X86ASM -DCRYPTOPP_DISABLE_SSSE3 -DCRYPTOPP_DISABLE_AESNI -D__ILP32__=0 -D_ILP32=0 -Wno-macro-redefined -fexceptions -frtti -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections
LOCAL_SRC_FILES := $(addprefix cryptopp/, 3way.cpp crc.cpp eax.cpp idea.cpp mqv.cpp rabin.cpp safer.cpp socketft.cpp twofish.cpp zinflate.cpp bfinit.cpp cryptlib_bds.cpp ec2n.cpp gcm.cpp integer.cpp nbtheory.cpp randpool.cpp salsa.cpp sosemanuk.cpp zlib.cpp adler32.cpp blowfish.cpp cryptlib.cpp eccrypto.cpp gf2_32.cpp iterhash.cpp network.cpp rc2.cpp seal.cpp square.cpp algebra.cpp blumshub.cpp ecp.cpp gf256.cpp luc.cpp oaep.cpp rc5.cpp seed.cpp squaretb.cpp algparam.cpp camellia.cpp default.cpp elgamal.cpp gf2n.cpp mars.cpp osrng.cpp rc6.cpp serpent.cpp strciphr.cpp vmac.cpp arc4.cpp cast.cpp des.cpp emsa2.cpp gfpcrypt.cpp marss.cpp panama.cpp rdtables.cpp sha3.cpp tea.cpp wait.cpp asn.cpp casts.cpp dessp.cpp eprecomp.cpp gost.cpp md2.cpp pch.cpp shacal2.cpp wake.cpp authenc.cpp cbcmac.cpp dh2.cpp esign.cpp gzip.cpp md4.cpp pkcspad.cpp rijndael.cpp sha.cpp tftables.cpp whrlpool.cpp base32.cpp ccm.cpp dh.cpp files.cpp hex.cpp md5.cpp polynomi.cpp ripemd.cpp sharkbox.cpp tiger.cpp winpipes.cpp base64.cpp channels.cpp dll.cpp filters.cpp hmac.cpp misc.cpp pssr.cpp rng.cpp shark.cpp tigertab.cpp xtr.cpp basecode.cpp cmac.cpp fips140.cpp hrtimer.cpp modes.cpp pubkey.cpp rsa.cpp simple.cpp trdlocal.cpp xtrcrypt.cpp cpu.cpp dsa.cpp fipsalgt.cpp ida.cpp mqueue.cpp queue.cpp rw.cpp skipjack.cpp ttmac.cpp zdeflate.cpp)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_EXPORT_CFLAGS := -DCRYPTOPP_DISABLE_X86ASM -DCRYPTOPP_DISABLE_SSSE3 -DCRYPTOPP_DISABLE_AESNI -D__ILP32__=0 -D_ILP32=0 -Wno-macro-redefined

include $(BUILD_STATIC_LIBRARY)
