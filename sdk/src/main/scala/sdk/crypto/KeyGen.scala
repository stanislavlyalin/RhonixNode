package sdk.crypto

trait KeyGen {
  def newKeyPair: (SecKey, PubKey)
}
