LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := cryptopp

LOCAL_CFLAGS := -DCRYPTOPP_DISABLE_ASM -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections
LOCAL_SRC_FILES := $(addprefix cryptopp/, cryptlib.cpp cpu.cpp integer.cpp 3way.cpp adler32.cpp algebra.cpp algparam.cpp arc4.cpp aria.cpp aria_simd.cpp ariatab.cpp asn.cpp authenc.cpp base32.cpp base64.cpp basecode.cpp bfinit.cpp blake2.cpp blake2b_simd.cpp blake2s_simd.cpp blowfish.cpp blumshub.cpp camellia.cpp cast.cpp casts.cpp cbcmac.cpp ccm.cpp chacha.cpp chacha_avx.cpp chacha_simd.cpp cham.cpp cham_simd.cpp channels.cpp cmac.cpp crc.cpp crc_simd.cpp darn.cpp default.cpp des.cpp dessp.cpp dh.cpp dh2.cpp dll.cpp donna_32.cpp donna_64.cpp donna_sse.cpp dsa.cpp eax.cpp ec2n.cpp eccrypto.cpp ecp.cpp elgamal.cpp emsa2.cpp eprecomp.cpp esign.cpp files.cpp filters.cpp fips140.cpp fipstest.cpp gcm.cpp gcm_simd.cpp gf256.cpp gf2_32.cpp gf2n.cpp gfpcrypt.cpp gost.cpp gzip.cpp hc128.cpp hc256.cpp hex.cpp hight.cpp hmac.cpp hrtimer.cpp ida.cpp idea.cpp iterhash.cpp kalyna.cpp kalynatab.cpp keccak.cpp lea.cpp lea_simd.cpp luc.cpp mars.cpp marss.cpp md2.cpp md4.cpp md5.cpp misc.cpp modes.cpp mqueue.cpp mqv.cpp nbtheory.cpp neon_simd.cpp oaep.cpp osrng.cpp padlkrng.cpp panama.cpp pkcspad.cpp poly1305.cpp polynomi.cpp ppc_power7.cpp ppc_power8.cpp ppc_power9.cpp ppc_simd.cpp pssr.cpp pubkey.cpp queue.cpp rabbit.cpp rabin.cpp randpool.cpp rc2.cpp rc5.cpp rc6.cpp rdrand.cpp rdtables.cpp rijndael.cpp rijndael_simd.cpp ripemd.cpp rng.cpp rsa.cpp rw.cpp safer.cpp salsa.cpp scrypt.cpp seal.cpp seed.cpp serpent.cpp sha.cpp sha3.cpp sha_simd.cpp shacal2.cpp shacal2_simd.cpp shark.cpp sharkbox.cpp simeck.cpp simeck_simd.cpp simon.cpp simon128_simd.cpp simon64_simd.cpp skipjack.cpp sm3.cpp sm4.cpp sm4_simd.cpp sosemanuk.cpp speck.cpp speck128_simd.cpp speck64_simd.cpp square.cpp squaretb.cpp sse_simd.cpp strciphr.cpp tea.cpp tftables.cpp threefish.cpp tiger.cpp tigertab.cpp ttmac.cpp tweetnacl.cpp twofish.cpp vmac.cpp wake.cpp whrlpool.cpp xed25519.cpp xtr.cpp xtrcrypt.cpp zdeflate.cpp zinflate.cpp zlib.cpp)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_EXPORT_CFLAGS := -DCRYPTOPP_DISABLE_ASM

include $(BUILD_STATIC_LIBRARY)

