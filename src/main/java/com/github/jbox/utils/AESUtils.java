package com.github.jbox.utils;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import lombok.Cleanup;
import org.apache.commons.crypto.cipher.CryptoCipher;
import org.apache.commons.crypto.cipher.CryptoCipherFactory;
import org.apache.commons.crypto.cipher.CryptoCipherFactory.CipherProvider;
import org.apache.commons.crypto.utils.Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Properties;

import static com.github.jbox.utils.HexStr.*;

public class AESUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public static String encode(String secretKey, String src) {
        try {

            // init encipher
            Properties properties = new Properties();
            properties.setProperty(CryptoCipherFactory.CLASSES_KEY, CipherProvider.JCE.getClassName());
            @Cleanup CryptoCipher encipher = Utils.getCipherInstance(ALGORITHM, properties);
            encipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(getBytes(secretKey), "AES"),
                    new IvParameterSpec(getBytes(secretKey)));

            // encode
            byte[] input = getBytes(src);
            byte[] output = new byte[64];
            int updateBytes = encipher.update(input, 0, input.length, output, 0);
            int finalBytes = encipher.doFinal(input, 0, 0, output, updateBytes);

            // to hexStr
            return toHexStr(output, updateBytes + finalBytes);
        } catch (Throwable t) {
            // IOException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, ShortBufferException, IllegalBlockSizeException
            throw new RuntimeException(t);
        }
    }

    public static String decode(String secretKey, String src) {
        try {
            Properties properties = new Properties();
            properties.setProperty(CryptoCipherFactory.CLASSES_KEY, CipherProvider.JCE.getClassName());
            @Cleanup CryptoCipher decipher = Utils.getCipherInstance(ALGORITHM, properties);
            decipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(getBytes(secretKey), "AES"),
                    new IvParameterSpec(getBytes(secretKey)));
            byte[] decoded = new byte[64];
            byte[] bytes = fromHexStr(src);

            int size = decipher.doFinal(bytes, 0, bytes.length, decoded, 0);
            return new String(decoded, 0, size);
        } catch (Throwable t) {
            // IOException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, ShortBufferException, IllegalBlockSizeException
            throw new RuntimeException(t);
        }
    }
}