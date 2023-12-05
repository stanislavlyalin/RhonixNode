package sdk.crypto

// Elliptic Curve Digital Signature Algorithm
trait ECDSA extends KeyGen with Signer with Verifier {
  // Size of data for signing / signature verification
  val dataSize: Int
}
