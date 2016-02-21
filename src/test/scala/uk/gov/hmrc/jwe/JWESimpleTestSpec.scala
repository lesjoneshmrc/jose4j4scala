/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.jwe

import org.jose4j.keys.AesKey
import org.jose4j.lang.{IntegrityException, ByteUtil}
import uk.gov.hmrc.play.test.UnitSpec
import org.jose4j.jwe.{JsonWebEncryption,KeyManagementAlgorithmIdentifiers, ContentEncryptionAlgorithmIdentifiers}
import java.nio.charset.Charset

class JWESimpleTestSpec extends UnitSpec {

  def getKey(keytext: String) = { new AesKey(keytext.getBytes(Charset.forName("ascii"))) }
  def getRandomKey() = { new AesKey(ByteUtil.randomBytes(16)) }

  def getEncryptor(key: AesKey) = {
    val encryptor = new JsonWebEncryption()
    encryptor.setKey(key)
    encryptor.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW)
    encryptor.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256)
    encryptor
  }

  "JWE Library" should {

    "pull back a consistent payload after encrypt / decrypt" in {
      val keytext = "PASSWORDpassword"
      val jwe = getEncryptor(getKey(keytext))
      jwe.setPayload("Wibble")
      val jweToken = jwe.getCompactSerialization()

      val jwe2 = getEncryptor(getKey(keytext))
      jwe2.setCompactSerialization(jweToken)

      jwe2.getPayload() shouldBe "Wibble"
    }

    "Not be able to pull back a payload after for different decryption key" in {
      val jwe = getEncryptor(getKey("PASSWORDpassword"))
      jwe.setPayload("Wibble")
      val jweToken = jwe.getCompactSerialization()

      val jwe2 = getEncryptor(getRandomKey())
      jwe2.setCompactSerialization(jweToken)

      intercept[IntegrityException] {
        jwe2.getPayload() shouldBe "Wibble"
      }
    }

    "pull back the same payload after encrypt / decrypt" in {

      val key = getKey("PASSWORDpassword")
      val jwe = new JsonWebEncryption()
      jwe.setPayload("Wibble")
      jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW)
      jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256)
      jwe.setKey(key)
      val jweToken = jwe.getCompactSerialization()

      // deliberately break the key
      //val key2 = new AesKey(ByteUtil.randomBytes(16))
      val key2 = key
      val jwe2 = new JsonWebEncryption()
      jwe2.setKey(key2)
      jwe2.setCompactSerialization(jweToken)

      val payload = jwe2.getPayload()

      payload shouldBe "Wibble"
    }
  }
}
