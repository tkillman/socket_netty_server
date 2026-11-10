# 인증 및 인가

## application-local.yml
jwt:
  access-token-key: 
  issuer: 
  audience: 

# powershell 초 만들기
[DateTimeOffset]::UtcNow.AddMinutes(10).ToUnixTimeSeconds()

# 포멧
{
  "iss": "",
  "aud": "",
  "sub": "",
  "iat": 1762760000,
  "exp": 1762760600
}