
package io.agora.iotlink.callkit;


import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.agora.iotlink.ICallkitMgr;


/**
 * @brief 会话管理器
 */
public class SessionMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/SessionMgr";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private HashMap<UUID, SessionCtx> mSessionMap = new HashMap<>();  ///< 会话映射表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 增加节点映射
     * @param sessionCtx : 要映射的会话
     * @return None
     */
    public void addSession(final SessionCtx sessionCtx) {
        synchronized (mSessionMap) {
            mSessionMap.put(sessionCtx.mSessionId, sessionCtx);
        }
    }

    /**
     * @brief 更新已经存在的节点信息
     * @param sessionCtx : 要更新的会话
     * @return None
     */
    public void updateSession(final SessionCtx sessionCtx) {
        synchronized (mSessionMap) {
            SessionCtx tmpSession = mSessionMap.get(sessionCtx.mSessionId);
            if (tmpSession == null) {
                return;
            }
            mSessionMap.put(sessionCtx.mSessionId, sessionCtx);
        }
    }

    /**
     * @brief 根据 sessionId 获取会话信息
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx getSession(final UUID sessionId) {
        synchronized (mSessionMap) {
            SessionCtx sessionCtx = mSessionMap.get(sessionId);
            return sessionCtx;
        }
    }

    /**
     * @brief 根据 设备 NodeId 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx findSessionByDevNodeId(final String devNodeId) {
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                if (devNodeId.compareToIgnoreCase(sessionCtx.mDevNodeId) == 0) {
                    return sessionCtx;
                }
            }
        }

        return null;
    }

    /**
     * @brief 根据 traceId 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx findSessionByTraceId(final long traceId) {
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                if (traceId == sessionCtx.mTraceId) {
                    return sessionCtx;
                }
            }
        }

        return null;
    }

    /**
     * @brief 根据 seesionId 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx findSessionBySessionId(final UUID sessionId) {
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                if (sessionId.equals(sessionCtx.mSessionId)) {
                    return sessionCtx;
                }
            }
        }

        return null;
    }

    /**
     * @brief 根据 频道名 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx findSessionByChannelName(final String chnName) {
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                if (sessionCtx.mChnlName == null) {
                    continue;
                }
                if (chnName.compareToIgnoreCase(sessionCtx.mChnlName) == 0) {
                    return sessionCtx;
                }
            }
        }

        return null;
    }

    /**
     * @brief 根据 sessionId 删除会话信息
     * @return 返回删除的会话，如果未找到则返回null
     */
    public SessionCtx removeSession(final UUID sessionId) {
        synchronized (mSessionMap) {
            SessionCtx sessionCtx = mSessionMap.remove(sessionId);
            return sessionCtx;
        }
    }


    /**
     * @brief 主叫返回结果
     */
    public static class QueryTimeoutResult {
        public List<SessionCtx> mDialTimeoutList = new ArrayList<>();
        public List<SessionCtx> mAnswerTimeoutList = new ArrayList<>();
        public List<SessionCtx> mDevOnlineTimeoutList = new ArrayList<>();
    }


    /**
     * @brief 查询所有 呼叫超时 或者 接听超时
     * @return 返回超时的会话列表
     */
    public QueryTimeoutResult queryTimeoutSessionList(long dialTimeout, long answerTimeout,
                                                    long devOnlineTimeout) {
        QueryTimeoutResult queryResult = new QueryTimeoutResult();
        long currTimestamp = System.currentTimeMillis();

        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();

                if ((sessionCtx.mState == ICallkitMgr.SESSION_STATE_DIAL_REQING) ||
                    (sessionCtx.mState == ICallkitMgr.SESSION_STATE_DIALING)) {
                    long timeDiff = currTimestamp - sessionCtx.mTimestamp;
                    if (timeDiff > dialTimeout) {  // 呼叫超时
                        queryResult.mDialTimeoutList.add(sessionCtx);
                    }
                }

                if (sessionCtx.mState == ICallkitMgr.SESSION_STATE_INCOMING) {
                    long timeDiff = currTimestamp - sessionCtx.mTimestamp;
                    if (timeDiff > answerTimeout) {  // 接听超时
                        queryResult.mAnswerTimeoutList.add(sessionCtx);
                    }
                }

                if ((sessionCtx.mType == ICallkitMgr.SESSION_TYPE_INCOMING) && (!sessionCtx.mDevOnline))  {
                    long timeDiff = currTimestamp - sessionCtx.mTimestamp;
                    if (timeDiff > devOnlineTimeout) {  // 来电时，设备未上线超时
                        queryResult.mDevOnlineTimeoutList.add(sessionCtx);
                    }
                }

            }
        }

        return queryResult;
    }

    /**
     * @brief 获取当前所有会话列表
     * @return 返回所有会话列表
     */
    public List<SessionCtx> getAllSessionList() {
        ArrayList<SessionCtx> sessionList = new ArrayList<>();
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                sessionList.add(sessionCtx);
            }
        }

        return sessionList;
    }


    /**
     * @brief  获取映射表数量
     * @return
     */
    public int size() {
        synchronized (mSessionMap) {
            int count = mSessionMap.size();
            return count;
        }
    }

    /**
     * @brief 清空映射表
     * @return None
     */
    public void clear() {
        synchronized (mSessionMap) {
            mSessionMap.clear();
        }
    }




}