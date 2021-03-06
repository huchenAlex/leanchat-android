package com.avoscloud.chat;

import android.app.Application;
import android.os.StrictMode;
import android.text.TextUtils;

import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avoscloud.chat.friends.AddRequest;
import com.avoscloud.chat.model.LCIMRedPacketMessage;
import com.avoscloud.chat.model.LCIMRedPcketAckMessage;
import com.avoscloud.chat.model.LeanchatUser;
import com.avoscloud.chat.model.UpdateInfo;
import com.avoscloud.chat.redpacket.GetUserBeanCallback;
import com.avoscloud.chat.redpacket.GetUserInfoCallback;
import com.avoscloud.chat.redpacket.RedPacketUtils;
import com.avoscloud.chat.redpacket.UserBeanCallback;
import com.avoscloud.chat.redpacket.UserInfoCallback;
import com.avoscloud.chat.service.PushManager;
import com.avoscloud.chat.util.LeanchatUserProvider;
import com.avoscloud.chat.util.UserCacheUtils;
import com.avoscloud.chat.util.Utils;
import com.baidu.mapapi.SDKInitializer;
import com.yunzhanghu.redpacketsdk.RedPacket;
import com.yunzhanghu.redpacketsdk.bean.RPUserBean;

import java.util.ArrayList;
import java.util.List;

import cn.leancloud.chatkit.LCChatKit;

/**
 * Created by lzw on 14-5-29.
 */
public class App extends Application {
  public static boolean debug = true;
  public static App ctx;

  @Override
  public void onCreate() {
    super.onCreate();
    ctx = this;
    Utils.fixAsyncTaskBug();

    String appId = "x3o016bxnkpyee7e9pa5pre6efx2dadyerdlcez0wbzhw25g";
    String appKey = "057x24cfdzhffnl3dzk14jh9xo2rq6w1hy1fdzt5tv46ym78";

    LeanchatUser.alwaysUseSubUserClass(LeanchatUser.class);

    AVObject.registerSubclass(AddRequest.class);
    AVObject.registerSubclass(UpdateInfo.class);

    // 节省流量
    AVOSCloud.setLastModifyEnabled(true);

    AVIMMessageManager.registerAVIMMessageType(LCIMRedPacketMessage.class);
    AVIMMessageManager.registerAVIMMessageType(LCIMRedPcketAckMessage.class);
    LCChatKit.getInstance().setProfileProvider(new LeanchatUserProvider());
    LCChatKit.getInstance().init(this, appId, appKey);

    // 初始化红包操作
    initRedData();
    RedPacket.getInstance().initContext(ctx);

    PushManager.getInstance().init(ctx);
    AVOSCloud.setDebugLogEnabled(debug);
    AVAnalytics.enableCrashReport(this, !debug);
    initBaiduMap();
    if (App.debug) {
      openStrictMode();
    }
  }

  /**
   * 初始化红包用户数据,在专属红包用到
   */
  private void initRedData() {
    /**
     * 根据一个群成员的id集合,查出群成员的具体信息,发专属红包时需要传群成员信息
     */
    RedPacketUtils.getInstance().setmGetUserInfoCallback(new GetUserInfoCallback() {
      @Override
      public void done(List<String> ids, final UserInfoCallback callback) {

        final List<RPUserBean> rpUserList = new ArrayList<RPUserBean>();

        UserCacheUtils.fetchUsers(ids, new UserCacheUtils.CacheUserCallback() {
          RPUserBean rpUserBean;

          @Override
          public void done(List<LeanchatUser> userList, Exception e) {
            if (userList != null) {
              for (int i = 0; i < userList.size(); i++) {
                rpUserBean = new RPUserBean();
                if (!LeanchatUser.getCurrentUserId().equals(userList.get(i).getObjectId())) {

                  rpUserBean.userId = userList.get(i).getObjectId();
                  rpUserBean.userNickname = userList.get(i).getUsername();
                  if (!TextUtils.isEmpty(userList.get(i).getAvatarUrl())) {
                    rpUserBean.userAvatar = userList.get(i).getAvatarUrl();
                  } else {
                    rpUserBean.userAvatar = "none";
                  }
                  rpUserList.add(rpUserBean);
                }
              }
            }
            callback.getUserInfo(rpUserList);
          }
        });
      }
    });

    /**
     * 根据人员id,查出人员的具体信息,拆除专属红包时需要传接收人的姓名和头像
     */
    RedPacketUtils.getInstance().setmGetUserBeanCallback(new GetUserBeanCallback() {
      RPUserBean rpUserBean;

      @Override
      public void done(String id, UserBeanCallback callback) {
        rpUserBean = new RPUserBean();
        if (!TextUtils.isEmpty(id)) {
          rpUserBean.userId = id;
        }
        if (UserCacheUtils.getCachedUser(id) != null) {
          if (!TextUtils.isEmpty(UserCacheUtils.getCachedUser(id).getUsername())) {

            rpUserBean.userNickname = UserCacheUtils.getCachedUser(id).getUsername();
          }
          if (!TextUtils.isEmpty(UserCacheUtils.getCachedUser(id).getAvatarUrl())) {

            rpUserBean.userAvatar = UserCacheUtils.getCachedUser(id).getAvatarUrl();
          } else {
            rpUserBean.userAvatar = "none";
          }
        }
        callback.getUserInfo(rpUserBean);
      }
    });
  }


  public void openStrictMode() {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()   // or .detectAll() for all detectable problems
            .penaltyLog()
            .build());
    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            //.penaltyDeath()
            .build());
  }

  private void initBaiduMap() {
    SDKInitializer.initialize(this);
  }
}
