package io.agora.iotlink.aws;

import com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider;
import com.amazonaws.regions.Regions;

public class AWSDeveloperIdentityProvider extends AWSAbstractCognitoDeveloperIdentityProvider {

    private static final String developerProvider = "granwin";

    public AWSDeveloperIdentityProvider(String identityId, String token, String accountId, String identityPoolId,
                                         String regin) {
        // 根据用户身份标志+身份池ID+服务区域验证开发者身份
        super(accountId, identityPoolId, Regions.fromName(regin));
        // 更新用户池ID和token令牌
        update(identityId,token);
    }
    @Override
    public String getProviderName() {
        return developerProvider;
    }

    // Use the refresh method to communicate with your backend to get an
    // identityId and token.

    @Override
    public String refresh() {
        update(identityId, token);
        return token;

    }

    // If the app has a valid identityId return it, otherwise get a valid
    // identityId from your backend.

    @Override
    public String getIdentityId() {
        return identityId;

    }
}
