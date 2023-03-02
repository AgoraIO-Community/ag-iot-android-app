#ifndef __BASE_COMMDEFINE_HPP__
#define __BASE_COMMDEFINE_HPP__

#include "stdint.h"
#include <jni.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <ctime>
#include <string>
#include <vector>
#include <list>
#include <tuple>
#include <mutex>
#include <thread>
#include <functional>
#include <cstdint>
#include <map>
#include <condition_variable>
#include <sys/time.h>
#include <android/log.h>

#ifndef DELETE_CTOR_COPY
#define DELETE_CTOR_COPY(className)      \
  className(const className &) = delete; \
  className &operator=(const className &) = delete
#endif

#ifndef DELETE_COPY
#define DELETE_COPY(className) \
  className() = default;       \
  DELETE_CTOR_COPY(className)
#endif


#define LOG_TAG         "SoftCodec"

#ifndef LOGD
#define LOGD(...)       __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#endif

#ifndef LOGE
#define LOGE(...)       __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif

#ifndef LOGI
#define LOGI(...)       __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#endif


static inline int64_t OSGetSysTimestamp()
{
  timeval now {0, 0};

  gettimeofday(&now, nullptr);
  return now.tv_sec * 1000L + (now.tv_usec + 500) / 1000;
}


template<typename T>
using SharePtr = std::shared_ptr<T>;

template<typename T>
using WeakPtr = std::weak_ptr<T>;

template<typename T>
using UniquePtr = std::unique_ptr<T>;

template<typename T>
using ListSharePtr = std::list<std::shared_ptr<T>>;


#define MakeSharePtr std::make_shared
#define MakeUniquePtr std::make_unique



template<class F>
struct function_traits;


template<class R, class... Args>
struct function_traits<R (*)(Args...)> : public function_traits<R(Args...)>
{
};


template<class R, class... Args>
struct function_traits<R(Args...)>
{
  using return_type                 = R;
  static constexpr std::size_t size = sizeof...(Args);
  template<std::size_t N>
  struct argument
  {
    static_assert(N < size, "error: invalid parameter index.");
    using type = typename std::tuple_element<N, std::tuple<Args...>>::type;
  };
};

template<auto Fn>
struct PointerDel final
{
  template<typename T>
  void operator()(T *p) const
  {
    if (!p || !Fn)
      return;

    using Traits = function_traits<decltype(Fn)>;
    if constexpr (std::is_same_v<typename Traits::template argument<0>::type, T **>)
      Fn(&p);
    else if constexpr (std::is_same_v<typename Traits::template argument<0>::type, T *>)
      Fn(p);
    else
      assert(false);
  }
};







#endif  // __BASE_COMMDEFINE_HPP__
