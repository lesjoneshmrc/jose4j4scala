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

import java.nio.charset.{StandardCharsets=>CS}

import org.jose4j.jwe.{ContentEncryptionAlgorithmIdentifiers => CEAI}
import org.jose4j.jwe.{KeyManagementAlgorithmIdentifiers => KMAI}
import org.jose4j.jwe.{JsonWebEncryption => JWE}
import org.jose4j.keys.AesKey
import play.api.libs.json
import play.api.libs.json.Json

trait JweCommon {
  protected def aeskey(keytext: String) = { new AesKey(keytext.getBytes(CS.ISO_8859_1)) }
}

object JweEncryptor extends JweCommon {

  def encrypt[A](key: String, payload: A)(implicit wts: json.Writes[A])  : String = {
    val encryptor = new JWE()
    encryptor.setKey(aeskey(key))
    encryptor.setAlgorithmHeaderValue(KMAI.DIRECT)  // COHO currently only support DIRECT
    encryptor.setEncryptionMethodHeaderParameter(CEAI.AES_128_CBC_HMAC_SHA_256)

    encryptor.setPayload(Json.stringify(wts.writes(payload)))

    encryptor.getCompactSerialization
  }
}

object JweDecryptor extends JweCommon {

  private[jwe] def decryptRaw[A](key: String, jweMessage:String) : String = {
    val decryptor = new JWE()
    decryptor.setKey(aeskey(key))
    decryptor.setCompactSerialization(jweMessage)
    decryptor.getPayload
  }

  def decrypt[A](key: String, jweMessage:String)(implicit rds: json.Reads[A]) : A = {
    unpack(decryptRaw(key, jweMessage))
  }

  private[jwe] def unpack[A](payload:String)(implicit rds: json.Reads[A]) : A = {
    Json.parse(payload).validate[A].fold(
      errs => throw new Exception(errs.toString()),  // JsValidationException("", url, mf.runtimeClass, errs),
      valid => valid
    )
  }
}
