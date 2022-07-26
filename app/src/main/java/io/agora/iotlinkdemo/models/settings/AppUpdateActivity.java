package io.agora.iotlinkdemo.models.settings;

import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.databinding.ActivityAppUpdateBinding;
import io.agora.iotlinkdemo.dialog.CheckUpdateDialog;
import io.agora.iotlinkdemo.manager.PagePathConstant;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * 应用更新
 */
@Route(path = PagePathConstant.pageAppUpdate)
public class AppUpdateActivity extends BaseViewBindingActivity<ActivityAppUpdateBinding> {
    private CheckUpdateDialog checkUpdateDialog;

    @Override
    protected ActivityAppUpdateBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityAppUpdateBinding.inflate(inflater);
    }

    @Override
    public void initListener() {
        getBinding().btnCheckUpdate.setOnClickListener(view -> {
            ToastUtils.INSTANCE.showToast(getString(R.string.function_not_open));
//            showCheckUpdateDialog();
        });
    }

    private void showCheckUpdateDialog() {


        if (checkUpdateDialog == null) {
            checkUpdateDialog = new CheckUpdateDialog(this);
            checkUpdateDialog.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {

                }
            });
        }
        checkUpdateDialog.show();
    }

}
