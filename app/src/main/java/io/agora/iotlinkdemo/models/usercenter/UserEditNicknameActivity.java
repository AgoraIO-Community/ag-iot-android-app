package io.agora.iotlinkdemo.models.usercenter;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityUserEditNicknameBinding;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 修改昵称
 */
@Route(path = PagePathConstant.pageUserEditNickname)
public class UserEditNicknameActivity extends BaseViewBindingActivity<ActivityUserEditNicknameBinding> {
    /**
     * 用户相关viewModel
     */
    private UserInfoViewModel userInfoViewModel;

    @Override
    protected ActivityUserEditNicknameBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityUserEditNicknameBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        userInfoViewModel = new ViewModelProvider(this).get(UserInfoViewModel.class);
        userInfoViewModel.setLifecycleOwner(this);
        userInfoViewModel.setISingleCallback((type, data) -> {
            if (type == Constant.CALLBACK_TYPE_USER_EDIT_USERINFO_SUCCESS) {
                ToastUtils.INSTANCE.showToast("修改成功");
                mHealthActivityManager.popActivity();
            }
        });
        getBinding().etNickname.setText(UserInfoViewModel.userInfo.mName);
    }

    @Override
    public void initListener() {
        getBinding().titleView.setRightIconClick(view -> {
            if (TextUtils.isEmpty(getBinding().etNickname.getText().toString())) {
                ToastUtils.INSTANCE.showToast("请输入昵称");
                return;
            }
            //执行保存用户昵称操作
            userInfoViewModel.requestSetUsername(UserInfoViewModel.userInfo, getBinding().etNickname.getText().toString());
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        userInfoViewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        userInfoViewModel.onStop();
    }
}
