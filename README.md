# 인증 및 인가

## application-local.yml
jwt:
  access-token-key: 
  issuer: 
  audience: 

# powershell 초 만들기
[DateTimeOffset]::UtcNow.AddMinutes(10).ToUnixTimeSeconds()