package io.agora.falcondemo.models.home;


import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.NetUtils;

import io.agora.falcondemo.R;
import io.agora.falcondemo.common.Constant;
import io.agora.falcondemo.dialog.CommonDialog;
import io.agora.falcondemo.utils.AppStorageUtil;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.AlarmVideoDownloader;
import io.agora.falcondemo.base.BaseViewBindingFragment;
import io.agora.falcondemo.databinding.FragmentHomeMineBinding;
import io.agora.falcondemo.models.settings.AboutActivity;
import io.agora.falcondemo.models.settings.AccountSecurityActivity;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.ICallkitMgr;


public class MineFragment extends BaseViewBindingFragment<FragmentHomeMineBinding>
        implements AlarmVideoDownloader.ICallback {
    private static final String TAG = "IOTLINK/MineFragment";

    Activity mOwnerActivity;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseFragment /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    protected FragmentHomeMineBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeMineBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        mOwnerActivity = this.getActivity();
        getBinding().ivToEdit.setVisibility(View.INVISIBLE);
        getBinding().vToEdit.setVisibility(View.INVISIBLE);

        String userId = AIotAppSdkFactory.getInstance().getLocalUserId();
        String nodeId = AIotAppSdkFactory.getInstance().getLocalNodeId();
        String txtName = userId + "\n (" + nodeId + ")";
        getBinding().tvUserMobile.setText(txtName);
    }

    @Override
    public void initListener() {
        getBinding().tvAccountSecrutiy.setOnClickListener(view -> {
            gotoAccountSecurityActivity();
        });

        getBinding().tvClearAppData.setOnClickListener(view -> {
            onBtnClearAppData();
        });


        getBinding().tvAbout.setOnClickListener(view -> {
            gotoAboutActivity();
        });
     }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    void gotoAccountSecurityActivity() {
        Intent intent = new Intent(getActivity(), AccountSecurityActivity.class);
        startActivity(intent);
    }

    void gotoAboutActivity() {
        Intent intent = new Intent(getActivity(), AboutActivity.class);
        startActivity(intent);
    }

    void onBtnClearAppData() {

        CommonDialog commonDlg = new CommonDialog(mOwnerActivity);
        commonDlg.setDialogTitle("清除应用配置后进程退出,下次进入需要重新配置appId, 确定清除？");
        commonDlg.setDialogBtnText(getString(R.string.cancel), getString(R.string.confirm));
        commonDlg.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
            @Override
            public void onLeftButtonClick() {
            }

            @Override
            public void onRightButtonClick() {
                // 进行清除操作
                AppStorageUtil.safePutString(mOwnerActivity, Constant.APP_ID, "");
                AppStorageUtil.safePutString(mOwnerActivity, Constant.ACCOUNT, "");
                AppStorageUtil.deleteAllValue();

                AIotAppSdkFactory.getInstance().release();
                System.exit(0);
            }
        });

        commonDlg.setCanceledOnTouchOutside(false);
        commonDlg.show();
    }

}
