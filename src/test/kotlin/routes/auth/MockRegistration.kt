package routes.auth

internal object MockRegistration {
    const val username = "mack@fortedigital.no"
    val publicKeyRequestOptions = """
                    {
                        "rp": {
                            "name": "TimeButler Auth",
                            "id": "localhost"
                        },
                        "user": {
                            "name": "$username",
                            "displayName": "$username",
                            "id": "OTY4NDY5NzMtMzdlZi00OTc1LTlmNDctNDg3MjZkMzlkOTVi"
                        },
                        "challenge": "yy7tWio1z6F5rru0J8BBFzg_PfuwOYqfgZVjeacxP3E",
                        "pubKeyCredParams": [
                            {
                                "alg": -7,
                                "type": "public-key"
                            },
                            {
                                "alg": -257,
                                "type": "public-key"
                            }
                        ],
                        "excludeCredentials": [],
                        "authenticatorSelection": {
                            "requireResidentKey": false,
                            "residentKey": "preferred",
                            "userVerification": "preferred"
                        },
                        "attestation": "none",
                        "extensions": {
                            "credProps": true
                        }
                    }
                    """.trimIndent()
    val publicKeyResponse = """{\"type\":\"public-key\",\"id\":\"_C-P0bfGluxXiE9TUDMAU9ceUm1_3xa0MhiB7kv_cGw\",\"rawId\":\"_C-P0bfGluxXiE9TUDMAU9ceUm1_3xa0MhiB7kv_cGw\",\"authenticatorAttachment\":\"platform\",\"response\":{\"clientDataJSON\":\"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoieXk3dFdpbzF6NkY1cnJ1MEo4QkJGemdfUGZ1d09ZcWZnWlZqZWFjeFAzRSIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6NTE3MyIsImNyb3NzT3JpZ2luIjpmYWxzZSwib3RoZXJfa2V5c19jYW5fYmVfYWRkZWRfaGVyZSI6ImRvIG5vdCBjb21wYXJlIGNsaWVudERhdGFKU09OIGFnYWluc3QgYSB0ZW1wbGF0ZS4gU2VlIGh0dHBzOi8vZ29vLmdsL3lhYlBleCJ9\",\"attestationObject\":\"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVikSZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NFAAAAAQECAwQFBgcIAQIDBAUGBwgAIPwvj9G3xpbsV4hPU1AzAFPXHlJtf98WtDIYge5L_3BspQECAyYgASFYINNXcd-xwRAoa-fGWSux1A4bHkmFkSahxdMLBKgpVlNZIlggHaoMlKBKh6f_Tstbvmto0RpHuRRWJNYOJmI-3ZOBNqA\",\"transports\":[\"internal\"]},\"clientExtensionResults\":{\"credProps\":{\"rk\":true}}}"""
}