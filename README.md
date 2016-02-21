
Jose4J4Scala Library
====

A sample Scala wrapper over the [jose4j]() library, proving JWE encryption and decryption capabilities - using the
compact serialization format as defined in [RFC7516](https://tools.ietf.org/html/rfc7516).

## Usage

The examples below assume a case class exists called Wibble, and that an implicit Json formatter is in scope; 
for example :

```scala
case class Wibble( x: String, y: String )

implicit val wibbleFormatter = Json.format[Wibble]
```

The key also needs to be the correct length. For example :

For a AES-128 key:
```scala
val key = "0123456789ABCDEF"
```

For a AES-256 key:
```scala
val key = "0123456789ABCDEF0123456789ABCDEF"
```

### Encryption

Encrypting a case class is as simple as having an implicit formatter in scope, and proving the data with a key.

```scala
val payload = Wibble("foo", "bar")
val token : String = JweEncryptor.encrypt[Wibble](key, payload)
```

### Decryption

Decrypting to a case class is also simple, requiring the token and a key :
```scala
val payload : Wibble = JweDecryptor.decrypt[Wibble](key, token)
```