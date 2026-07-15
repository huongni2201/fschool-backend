package com.fschool.edu.fschool_backend.infrastructure.security;

import java.time.Duration;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private static final String DEFAULT_PUBLIC_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3+n4KtX1TXVcKu+6bFc6
            9oYCMYZf7MLtz0pBRo6hx1M8u+OI5RW0+uyO7hXsIuG4sKdzgIQ5sZFBZ8Bhoig1
            UYgX1v41NeSu1MCwprLRlj+r/gTCvYCV8CFht+/+FL2KfVRMUjsW90HkWvC06IzL
            LrVks72xPfVRL1JC9cFRYG3mnfOUQeAGDak52WON6wy8uIPfOzpByxSMHhn0j2C+
            oBaeNl1doIjVEBJhw1IP4FEoUM2F44sx1dRTJqz70Bn+ZblBR2rSTWDoHMnjZgo1
            qieO7GiYzGa1SKA9wdoU6Hc4lV7qfE3ONmG2GxZLHHSF+MWY/XX46dUsZr0f91lg
            zQIDAQAB
            -----END PUBLIC KEY-----""";

    private static final String DEFAULT_PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDf6fgq1fVNdVwq
            77psVzr2hgIxhl/swu3PSkFGjqHHUzy744jlFbT67I7uFewi4biwp3OAhDmxkUFn
            wGGiKDVRiBfW/jU15K7UwLCmstGWP6v+BMK9gJXwIWG37/4UvYp9VExSOxb3QeRa
            8LTojMsutWSzvbE99VEvUkL1wVFgbead85RB4AYNqTnZY43rDLy4g987OkHLFIwe
            GfSPYL6gFp42XV2giNUQEmHDUg/gUShQzYXjizHV1FMmrPvQGf5luUFHatJNYOgc
            yeNmCjWqJ47saJjMZrVIoD3B2hTodziVXup8Tc42YbYbFkscdIX4xZj9dfjp1Sxm
            vR/3WWDNAgMBAAECggEAatqxtmdyb6uA93B9q3QZTEn5Q/8XSdpgOFr8OZqZc5Cy
            XAuiaYhpXJGFR/MoRCEpzQusH/l5utZRuxxgu0yq3SSwE8YuSHDASgFGi9asFCKA
            hnFpKZvycazMgEXEH8uwCk0vOtK+C/Li8c8K1itSTxD7ZJcyvRmTy5xbHgJKPlRv
            duZMDUumP6NWUsf87UDKDndOukEPIPR8qrLn9haULYnVWN12rJMdf3mwQ+yiSKHf
            ushAtEJbcZGDzutciqj0sLQVBfk8lrUQ9EoCswq/g3foXqt/uIiYMWmnhIKv4INg
            MbtuIDqlU6tZka5HEk+jJk7icA6kfJgIT+wmeRQgFQKBgQD0217AAD5CmzNUc+j1
            RosemVHhR4ZKO3lZAq9GEtp6kgPhkPVSOtWv/1HcByxAFAyujlpzhx2O59URSi1l
            yndqKNiUs7vKxaRgXS5xVimcoOeCb+ihdzxBdQtoWcuftopw6fBs/SCyEkDuiV3r
            gAK1NM4u04BZDqWcolGZzhA1ywKBgQDqGpwS2pZyXBOjY1C8/gIkoZxxNOsRgCOO
            VRpkraH835JQ+A51YdaFjxObWxwoy9Dk4qjOI6libc4ZvamS2IuoLBMjwbEnLPZt
            wbo/XGQqjBEOsynT0uyt2Ewlupz+/tZjD7by8cY/OsWK5f2dwihIJH9JfwnUch8L
            S1ZHksywxwKBgHXKPidxNoJGNQgwhmXP3McIjKoqBUEaoUKflv/HvBBww6s0V7+K
            DBgRZCjLzo641mz2kC65Vg/UGNyraXFpV87a/W1zlShzsaKl0Ny2zWfKw/Qsr6VV
            PchjbOxzHhCj+K9e6skTcDIZs+DMdkD/IM5ZK8K8/iZZdHQIZebuiT/HAoGAN//n
            h8cMKA0IkZQHUz47ywFxx87N0GDjoH+REbZLQo9Ek+PSqZee1lIUcZxIzyV6MdZa
            ZP043pe/rn6lGsB8H91zMqF5vBJQXI7z+4YhW+AnkGmhPs982FUeWgQa3BCfvhCb
            ReA5+RQY/xHnKh6wvhkk7bLa6hvmezApUnO2TS8CgYEA819s9j4V0Zyix6Ek/muO
            vseAEix4qaWtyx0xel0mvRmQ7DQ3L6IjI8BwDMeJP6IjTpQ8ygVz/S4Dd+nVQ6D1
            qdXOSjt9weoABTVqcV8o/VOXHKDPRLdpD7pAi9x6uTV54lt4GqOo1aNkO0Mb13wm
            dQ4/mVemmrxVpuOuw3/jrLY=
            -----END PRIVATE KEY-----""";

    private String issuer = "fschool-backend";
    private Duration accessTokenTtl = Duration.ofHours(1);
    private Duration refreshTokenTtl = Duration.ofDays(30);
    private String publicKey = DEFAULT_PUBLIC_KEY;
    private String privateKey = DEFAULT_PRIVATE_KEY;

    public void setIssuer(String issuer) {
        if (hasText(issuer)) {
            this.issuer = issuer;
        }
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        if (accessTokenTtl != null) {
            this.accessTokenTtl = accessTokenTtl;
        }
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        if (refreshTokenTtl != null) {
            this.refreshTokenTtl = refreshTokenTtl;
        }
    }

    public void setPublicKey(String publicKey) {
        if (hasText(publicKey)) {
            this.publicKey = publicKey;
        }
    }

    public void setPrivateKey(String privateKey) {
        if (hasText(privateKey)) {
            this.privateKey = privateKey;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
