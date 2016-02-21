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

import java.nio.charset.StandardCharsets.ISO_8859_1
import java.util.Base64

import org.jose4j.jwe.{ContentEncryptionAlgorithmIdentifiers => CEAI}
import org.jose4j.jwe.{KeyManagementAlgorithmIdentifiers => KMAI}
import org.jose4j.jwe.{JsonWebEncryption => JWE}
import org.jose4j.keys.AesKey
import org.jose4j.lang.ByteUtil
import org.scalatest.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

case class Wibble( x: String, y: String )

class JWEScalaTestSpec extends UnitSpec with Matchers {

  implicit val wibbleFormatter = Json.format[Wibble]

  val sample1 = s"""{"x":"nermal", "y":"foo"}"""
  val JwePattern = """^[^.]*.[^.]*.[^.]*.[^.]*.[^.]*$"""
  val JweGroupsExp = """^([^.]*).([^.]*).([^.]*).([^.]*).([^.]*)$""".r
  val Base64chars = """[A-Za-z0-9.=_-]+"""

  private def B64decode( bytes: String ) : String = {
    new String(Base64.getDecoder.decode(bytes).map(_.toChar))
  }

  private def getKey(keytext: String) = { new AesKey(keytext.getBytes(ISO_8859_1)) }
  private def getRandomKey() = { new AesKey(ByteUtil.randomBytes(16)) }

  "JWE Decryptor unpack" should {
    "produce a valid Wibble case class from sample JSON" in {
      val wibble = JweDecryptor.unpack[Wibble]( sample1 )
      wibble shouldBe Wibble("nermal", "foo")
    }
  }

  "JWE Encryptor when encrypting a payload" should {

    val key1 = "0123456789ABCDEF0123456789ABCDEF"

    "produce a valid JOSE header" in {
      val token = JweEncryptor.encrypt[Wibble](key1, Wibble("foo", "bar"))

      token should fullyMatch regex JwePattern
      token should fullyMatch regex Base64chars
      token shouldBe a [String]

      val JweGroupsExp(header, _, _, _, _) = token
      header should startWith ("eyJhbG")

      val json = Json.parse(B64decode(header))

      (json \ "alg").asOpt[String].getOrElse("") shouldBe "dir"
      (json \ "enc").asOpt[String].getOrElse("") shouldBe "A128CBC-HS256"
    }

    "contain expected JSON payload" in {
      val payload = Wibble("foo", "bar")
      val token = JweEncryptor.encrypt[Wibble](key1, payload)

      token should fullyMatch regex JwePattern

      JweDecryptor.decryptRaw(key1, token) shouldBe """{"x":"foo","y":"bar"}"""
    }

    "give back the original payload from a Wibble case class instance" in {
      val payload = Wibble("foo", "bar")
      val token = JweEncryptor.encrypt[Wibble](key1, payload)

      token should fullyMatch regex JwePattern

      JweDecryptor.decrypt[Wibble](key1, token) shouldBe payload
    }

  }

  "JWE Decryptor when decrypting a payload" should {

    def getTestEncryptor(key: AesKey, headerAlg : String = KMAI.DIRECT) = {
      val encryptor = new JWE()
      encryptor.setKey(key)
      encryptor.setAlgorithmHeaderValue(headerAlg)
      encryptor.setEncryptionMethodHeaderParameter(CEAI.AES_128_CBC_HMAC_SHA_256)
      encryptor
    }

    "pull back a consistent payload after encrypt / decrypt for A128KW header" in {
      val keytext = "PASSWORD££££££££"
      val jwe = getTestEncryptor(getKey(keytext), KMAI.A128KW)
      jwe.setPayload(sample1)
      val jweToken = jwe.getCompactSerialization()

      JweDecryptor.decrypt[Wibble](keytext, jweToken) shouldBe Wibble("nermal", "foo")
    }

    "pull back a consistent payload after encrypt / decrypt for DIRECT header" in {
      val keytext = "PASSWORDpasswordPASSWORDpassword"
      val jwe = getTestEncryptor(getKey(keytext), KMAI.DIRECT)
      jwe.setPayload(sample1)
      val jweToken = jwe.getCompactSerialization()

      JweDecryptor.decrypt[Wibble](keytext, jweToken) shouldBe Wibble("nermal", "foo")
    }

    "check an invalid JSON doc" in {
      val keytext = "PASSWORDpasswordPASSWORDpassword"
      val jwe = getTestEncryptor(getKey(keytext), KMAI.DIRECT)
      jwe.setPayload("""{xxx""")
      val jweToken = jwe.getCompactSerialization()

      intercept[Exception] {
        JweDecryptor.decrypt[Wibble](keytext, jweToken) shouldBe Wibble("nermal", "foo")
      }
    }

    "check valid but incorrect JSON" in {
      val keytext = "PASSWORDpasswordPASSWORDpassword"
      val jwe = getTestEncryptor(getKey(keytext), KMAI.DIRECT)
      jwe.setPayload("""{"a":"b"}""")
      val jweToken = jwe.getCompactSerialization()

      intercept[Exception] {
        JweDecryptor.decrypt[Wibble](keytext, jweToken) shouldBe Wibble("nermal", "foo")
      }
    }
  }
}
