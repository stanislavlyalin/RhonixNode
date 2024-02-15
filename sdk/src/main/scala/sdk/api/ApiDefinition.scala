package sdk.api

import java.io.InputStream

final case class ApiDefinition[F[_], Req, Resp](
  endpointName: String,
  serializeA: Req => InputStream,
  parseA: InputStream => Req,
  callback: Req => F[Resp],
  serializeB: Resp => InputStream,
  parseB: InputStream => Resp,
)
