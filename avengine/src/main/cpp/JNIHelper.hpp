



#ifndef __JNIHELPER_HPP
#define __JNIHELPER_HPP


#if defined(__linux__) || defined(__ANDROID__)

#include <jni.h>
#include <string>
#include <type_traits>
#include <functional>
#include <memory>
#include <vector>
#include <map>


/** @brief Java bool primitive */
constexpr auto kTypeBool      = "Z";
constexpr auto kTypeBoolArray = "[Z";

/** @brief Java byte primitive */
constexpr auto kTypeByte      = "B";
constexpr auto kTypeByteArray = "[B";

/** @brief Java char primitive */
constexpr auto kTypeChar      = "C";
constexpr auto kTypeCharArray = "[C";

/** @brief Java short primitive */
constexpr auto kTypeShort      = "S";
constexpr auto kTypeShortArray = "[S";

/** @brief Java int primitive */
constexpr auto kTypeInt      = "I";
constexpr auto kTypeIntArray = "[I";

/** @brief Java long primitive */
constexpr auto kTypeLong      = "J";
constexpr auto kTypeLongArray = "[J";

/** @brief Java float primitive */
constexpr auto kTypeFloat      = "F";
constexpr auto kTypeFloatArray = "[F";

/** @brief Java double primitive */
constexpr auto kTypeDouble      = "D";
constexpr auto kTypeDoubleArray = "[D";


/** @brief Java void */
constexpr auto kTypeVoid = "V";


/** @brief Java String class */
constexpr auto kTypeString      = "Ljava/lang/String;";
constexpr auto kTypeStringArray = "[Ljava/lang/String;";

/** @brief Java base Object class */
constexpr auto kTypeObject      = "Ljava/lang/Object;";
constexpr auto kTypeObjectArray = "[Ljava/lang/Object;";

/**
 * jobject 对象转换为 cpp 对象的函数指针定义
 * @tparam CppType 返回的 C++ 类型
 */
template<typename CppType>
using JObj2CppObjConvertFunc = CppType (*)(JNIEnv *, jobject);

/**
 * JNI里的getField函数指针(GetObjectField...)
 * @tparam T 返回的 field类型
 */
template<typename T>
using JNIEnvGetFieldFunc = T (JNIEnv::*)(jobject, jfieldID);

/**
 * JNI里的getStaticField函数指针(GetStaticObjectField...)
 * @tparam T 返回的 field类型
 */
template<typename T>
using JNIEnvGetStaticFieldFunc = T (JNIEnv::*)(jclass, jfieldID);

/**
 * JNI里ReleasexxxxArrayElements 函数指针(ReleaseByteArrayElements...)
 * @tparam ARRAY 数组类型
 * @tparam ITEM 数组中对象类型
 */
template<typename ITEM, typename ARRAY>
using JNIEnvReleaseArrayElementsFunc = void (JNIEnv::*)(ARRAY, ITEM, jint);

/**
 * jni对象的uniqueptr封装
 * @tparam T 默认是jobject
 */
template<typename T = jobject>
using JObjectPtr = std::unique_ptr<typename std::remove_pointer_t<T>, std::function<void(T)>>;

/**
 * 获取jarray中jobject对象的函数指针
 * @tparam ITEM jobject雷牙乡
 * @tparam ARRAY jarray类型
 */
template<typename ITEM, typename ARRAY>
using GetArrayBufferFunc = std::unique_ptr<ITEM, std::function<void(ITEM *)>> (*)(JNIEnv *, ARRAY);

/**
 * JNIEnv智能指针 能自动处理线程关系
 */
#if 0
class JNIEnvPtr final
{
public:
  JNIEnvPtr(const JNIEnvPtr &) = delete;
  JNIEnvPtr &operator=(const JNIEnvPtr &) = delete;
  JNIEnvPtr()
  {
    if (getGlobalJavaVM()->GetEnv((void **)&env_, JNI_VERSION_1_6) == JNI_EDETACHED)
    {
#ifdef __ANDROID__
      getGlobalJavaVM()->AttachCurrentThread(&env_, nullptr);
#else
      getGlobalJavaVM()->AttachCurrentThread((void **)&env_, nullptr);
#endif
      need_detach_ = true;
    }
  }

  ~JNIEnvPtr()
  {
    if (need_detach_)
    {
      getGlobalJavaVM()->DetachCurrentThread();
    }
  }

  JNIEnv *operator->()
  {
    return env_;
  }

private:
  JNIEnv *env_ {nullptr};
  bool    need_detach_ {false};
};
#endif

/**
 * 将jstring 转换成 const char*的uniquePtr
 * @param env JNIEnv
 * @param jstr 待转换的jstring
 * @return const char*的uniquePtr
 */
static auto getRAIIJString(JNIEnv *env, jstring jstr)
{
  using jni_string_ptr = std::unique_ptr<const char[], std::function<void(const char *)>>;
  if (jstr)
  {
    return jni_string_ptr(env->GetStringUTFChars(jstr, nullptr), [env, jstr](const char *value) mutable {
      env->ReleaseStringUTFChars(jstr, value);
    });
  }
  else
  {
    return jni_string_ptr();
  }
}

/**
 * 把一个普通的jni对象包装成一个uniquePtr, 是为了避免忘记释放
 * @tparam T jni对象的类型
 * @tparam shouldDeleteRef 是否应该删除localRef, 默认为是
 * @param env JNIEnv
 * @param item jni对象
 * @return jni对象封装的uniquePtr
 */
template<typename T = jobject, bool shouldDeleteRef = true>
static auto getJNIObjPtr([[maybe_unused]] JNIEnv *env, T item)
{
  // jstring 需要延迟释放, 暂时不deleteLocalRef
  if constexpr (shouldDeleteRef && !std::is_same_v<T, jstring>)
  {
    return JObjectPtr<T>(item, [env](T obj) {
      env->DeleteLocalRef(obj);
    });
  }
  else
  {
    return JObjectPtr<T>(item, [](T obj) {});
  }
}

/**
 * 从一个jobjectarray中获取一个jobject对象的uniqueptr
 * @param env  JNIEnv
 * @param jarray array
 * @param index 取数组的第几个
 * @return jobject的uniqueptr
 */
template<typename T = jobject>
static inline auto GetObjectItem(JNIEnv *env, jobjectArray jarray, int index)
{
  return getJNIObjPtr<T, true>(env, (T)env->GetObjectArrayElement(jarray, index));
}


/**
 * 获取jni对象的基础类型(int, bool...)属性值
 * @tparam T 属性字段类型
 * @tparam isStatic 是否是静态字段
 * @tparam fun 获取字段的函数指针
 * @param env JNIEnv
 * @param jobj jni对象
 * @param pName 属性的 字段名
 * @param type JNI数据类型
 * @param fun JNIField获取函数 (普通函数, 非static)
 * @return 获取的属性字段值
 */
template<typename T, bool isStatic = false, auto fun = &JNIEnv::GetObjectField>
static inline T _GetField(JNIEnv *    env,
                          jobject     jobj,
                          const char *pName,
                          const char *type)
{
  jclass pClass = env->GetObjectClass(jobj);
  if (pClass == nullptr)
  {
    return T {};
  }

  jfieldID fieldId = nullptr;
  if constexpr (isStatic)
    fieldId = env->GetStaticFieldID(pClass, pName, type);
  else
    fieldId = env->GetFieldID(pClass, pName, type);

  env->DeleteLocalRef(pClass);

  return static_cast<T>((env->*fun)(jobj, fieldId));
}


/**
 * 把基础类型的jni array(jintArray...) 转换成 array[]的uniqueptr, 供c/cpp直接使用
 * @tparam ITEM ARRAY的条目类型 例如 jint
 * @tparam ARRAY jni array类型
 * @param env JNIEnv
 * @param array jni对象
 * @param item 获取的item的指针地址
 * @param func item获取后的释放函数
 * @return ITEM对应的裸类型数组的uniquePtr (uniqueptr (int *))
 */
template<typename ITEM, typename ARRAY>
static inline auto _GetJNIObjArrayPtr(JNIEnv *                                                        env,
                                      ARRAY                                                           array,
                                      ITEM *                                                          item,
                                      JNIEnvReleaseArrayElementsFunc<std::add_pointer_t<ITEM>, ARRAY> func)
{
  return std::unique_ptr<ITEM, std::function<void(ITEM *)>>(item, [env, array, func](ITEM *obj) {
    (env->*func)(array, obj, 0);
  });
}


/////////// Get JNI Field /////////////

/**
 * 获取jni对象的基础类型(int, bool...)属性值
 * @tparam T 基础类型的类型名
 * @tparam isStatic 是否是静态属性字段, 默认为否
 * @param env JNIEnv
 * @param jobj jni对象
 * @param pName 属性的 字段名
 * @return 对应属性的基础类型值
 */
template<typename T, bool isStatic = false>
static inline T GetJNIFieid(JNIEnv *env, jobject jobj, const char *pName)
{
  if constexpr (std::is_same_v<T, jint>)
    return _GetField<T, isStatic, &JNIEnv::GetIntField>(env, jobj, pName, kTypeInt);

  if constexpr (std::is_same_v<T, jboolean>)
    return _GetField<T, isStatic, &JNIEnv::GetBooleanField>(env, jobj, pName, kTypeBool);

  if constexpr (std::is_same_v<T, jchar>)
    return _GetField<T, isStatic, &JNIEnv::GetCharField>(env, jobj, pName, kTypeChar);

  if constexpr (std::is_same_v<T, jbyte>)
    return _GetField<T, isStatic, &JNIEnv::GetByteField>(env, jobj, pName, kTypeByte);

  if constexpr (std::is_same_v<T, jshort>)
    return _GetField<T, isStatic, &JNIEnv::GetShortField>(env, jobj, pName, kTypeShort);

  if constexpr (std::is_same_v<T, jlong>)
    return _GetField<T, isStatic, &JNIEnv::GetLongField>(env, jobj, pName, kTypeLong);

  if constexpr (std::is_same_v<T, jfloat>)
    return _GetField<T, isStatic, &JNIEnv::GetFloatField>(env, jobj, pName, kTypeFloat);

  if constexpr (std::is_same_v<T, jdouble>)
    return _GetField<T, isStatic, &JNIEnv::GetDoubleField>(env, jobj, pName, kTypeDouble);
}

/**
 * 获取jni对象的java类型(Object, String...)的属性值(对应jobject) 的 uniquePtr
 * @tparam T jni对象类型, 默认是jobject
 * @param env JNIEnv
 * @param jobj jni对象
 * @param pName 属性字段名
 * @param pPackName 属性字段的JNI签名
 * @return T类型的uniquePtr
 */
template<typename T = jobject, bool isStatic = false>
static inline auto GetJNIObjField(JNIEnv *env, jobject jobj, const char *pName, const char *pPackName)
{
  return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, pPackName));
}


/**
 * 获取jni对象的jstring属性值 的 uniquePtr
 * @tparam isStatic 是否是静态赎罪营
 * @param env JNIEnv
 * @param jobj jni对象
 * @param pName 属性字段名
 * @return jstring的uniquePtr
 */
template<bool isStatic = false>
static inline auto GetJStringField(JNIEnv *env, jobject jobj, const char *pName)
{
  return GetJNIObjField<jstring, isStatic>(env, jobj, pName, kTypeString);
}

/////////// Get JNI Field /////////////

/////////// Get JNI Array /////////////


/**
 * 获取jni对象的基础类型数组(int[], bool[]...)属性值 的uniquePtr
 * @tparam T 基础类型数组(jintArray...)
 * @tparam isStatic 是否是静态属性值
 * @param env JNIEnv
 * @param jobj jni对象
 * @param pName 属性字段名
 * @return T类型的uniquePtr
 */
template<typename T, bool isStatic = false>
static inline auto GetJNIArrayField(JNIEnv *env, jobject jobj, const char *pName)
{
  if constexpr (std::is_same_v<T, jintArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeIntArray));

  if constexpr (std::is_same_v<T, jbooleanArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeBoolArray));

  if constexpr (std::is_same_v<T, jbyteArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeByteArray));

  if constexpr (std::is_same_v<T, jcharArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeCharArray));

  if constexpr (std::is_same_v<T, jshortArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeShortArray));

  if constexpr (std::is_same_v<T, jlongArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeLongArray));

  if constexpr (std::is_same_v<T, jfloatArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeFloatArray));

  if constexpr (std::is_same_v<T, jdoubleArray>)
    return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, kTypeDoubleArray));
}


/**
 * 获取jni对象的Object数组(jobjectArray...)属性值 的uniquePtr
 * @tparam T 默认为jobjectArray
 * @param env JNIEnv
 * @param jobj jni对象
 * @param pName 属性字段名
 * @param pPackName 属性的JNI签名
 * @return T的uniquePtr
 */
template<typename T = jobjectArray, bool isStatic = false>
static inline auto GetJNIObjArrayFieid(JNIEnv *env, jobject jobj, const char *pName, const char *pPackName)
{
  return getJNIObjPtr<T>(env, _GetField<T, isStatic>(env, jobj, pName, pPackName));
}

/**
 * GetJNIObjArrayFieid 的 jstring 特例
 */
template<bool isStatic = false>
static inline auto GetJStringArrayField(JNIEnv *env, jobject jobj, const char *pName)
{
  return GetJNIObjArrayFieid<jobjectArray, isStatic>(env, jobj, pName, kTypeStringArray);
}


/////////// Get JNI Array /////////////

/////////// Convert JNI Array To Vector/////////////


/**
 * 把基础类型的jni array(jintArray...) 转换成 array[]的uniqueptr, 供c/cpp直接使用
 * @tparam ITEM ARRAY的条目类型 例如 jint
 * @tparam ARRAY jni array类型
 * @param env JNIEnv
 * @param array jni对象
 * @return ITEM对应的裸类型数组的uniquePtr (uniqueptr (int *))
 */
template<typename ITEM, typename ARRAY>
static inline auto GetJNIArrayBuffer(JNIEnv *env, ARRAY array)
{
  if constexpr (std::is_same_v<ARRAY, jintArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetIntArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseIntArrayElements);
  }
  if constexpr (std::is_same_v<ARRAY, jbooleanArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetBooleanArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseBooleanArrayElements);
  }
  if constexpr (std::is_same_v<ARRAY, jbyteArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetByteArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseByteArrayElements);
  }
  if constexpr (std::is_same_v<ARRAY, jcharArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetCharArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseCharArrayElements);
  }
  if constexpr (std::is_same_v<ARRAY, jshortArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetShortArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseShortArrayElements);
  }
  if constexpr (std::is_same_v<ARRAY, jlongArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetLongArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseLongArrayElements);
  }
  if constexpr (std::is_same_v<ARRAY, jfloatArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetFloatArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseFloatArrayElements);
  }
  if constexpr (std::is_same_v<ARRAY, jdoubleArray>)
  {
    return _GetJNIObjArrayPtr<ITEM, ARRAY>(env,
                                           array,
                                           env->GetDoubleArrayElements(array, nullptr),
                                           &JNIEnv::ReleaseDoubleArrayElements);
  }
}


/////////// Container Convert Funcs /////////////

/**
 * 基本类型的 jarray item to vector item
 * @tparam ITEM jobject类型
 * @tparam ARRAY  jarray 类型
 * @param env  JNIEnv
 * @param array jobjectarray
 * @return vector item
 */
template<typename ITEM, typename ARRAY>
static inline std::vector<ITEM> jPrimitiveArray_to_vector(JNIEnv *env, ARRAY array)
{
  std::vector<ITEM> vRet;
  if (array)
  {
    auto leftTop = GetJNIArrayBuffer<ITEM, ARRAY>(env, array);

    if (leftTop)
    {
      int arrLen = env->GetArrayLength(array);
      for (int i = 0; i < arrLen; i++)
        vRet.emplace_back(leftTop.get()[i]);
    }
  }

  return vRet;
}


/**
 * 将jobjectarray转换为vector<jobjectptr>
 * @param env  JNIEnv
 * @param list jobjectarray
 * @return vector<jobjectptr>
 */
static inline auto jobjArray_to_vector(JNIEnv *env, jobjectArray list)
{
  std::vector<JObjectPtr<>> vRet;
  if (list)
  {
    int arrLen = env->GetArrayLength(list);
    for (int i = 0; i < arrLen; i++)
    {
      auto up = GetObjectItem(env, list, i);
      if (up)
      {
        vRet.emplace_back(std::move(up));
      }
    }
  }

  return vRet;
}

/**
 * jstringarray to vector<std::string>
 * @param env  JNIEnv
 * @param array jstringarray
 * @return  vector<std::string>
 */
static inline std::vector<std::string> jStringArray_to_vector(JNIEnv *     env,
                                                              jobjectArray array)
{
  std::vector<std::string> vRet;
  if (array)
  {
    int arrLength = env->GetArrayLength(array);
    for (int i = 0; i < arrLength; i++)
    {
      auto string_ = GetObjectItem<jstring>(env, array, i);
      auto str     = getRAIIJString(env, string_.get());
      // 去除了null 字符串
      if (str)
      {
        vRet.emplace_back(str.get());
      }
    }
  }

  return vRet;
}

/**
 * java list<jobject> to vector<jobject>
 * @param env  JNIEnv
 * @param list java list
 * @return vector<jobject>
 */
static inline std::vector<jobject> javaList_to_vector(JNIEnv *env, jobject list)
{
  std::vector<jobject> vRet;
  if (list)
  {
    jclass    listClass = env->GetObjectClass(list);
    jmethodID size_mid  = env->GetMethodID(listClass, "size", "()I");
    jmethodID get_mid   = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
    jint      size      = env->CallIntMethod(list, size_mid);

    for (int i = 0; i < size; i++)
    {
      jobject jobj = env->CallObjectMethod(list, get_mid, i);
      vRet.emplace_back(jobj);
    }
  }
  return vRet;
}


/**
 * int8_t* to jbyteArray
 * @param env  JNIEnv
 * @param buf int8_t*
 * @return jbyteArray
 *
 */
static inline auto charArray_to_jbyteArray(JNIEnv *env, int8_t *buf, int len)
{
  return JObjectPtr<jbyteArray>([env, len, buf] {
    auto array = env->NewByteArray(len);
    env->SetByteArrayRegion(array, 0, len, buf);
    return array; }(), [env](jbyteArray array) mutable { env->DeleteLocalRef(array); });
}


/**
 * jbyteArray to uint8_t[]的uniquePtr
 * @param env  JNIEnv
 * @param array jbyteArray
 * @return uint8_t[]的uniquePtr
 *
 */
static inline auto jbyteArray_to_unsigned_char_array(JNIEnv *env, jbyteArray array)
{
  auto                     len = env->GetArrayLength(array);
  std::unique_ptr<jbyte[]> buf = std::make_unique<jbyte[]>(static_cast<size_t>(len));
  env->GetByteArrayRegion(array, 0, len, buf.get());
  return buf;
}


/**
 * java arraylist to cpp vector convert function
 * @tparam T 容器的C++ 对象类型
 * @param env JNIEnv
 * @param jarrayList java的 ArrayList jni指针
 * @param converter 对象转换函数
 * @return std::vector<T>
 */
template<typename T>
static inline std::vector<T> jArrayList_to_std_vector(JNIEnv *env, jobject jarrayList, JObj2CppObjConvertFunc<T> converter)
{
  jclass    cls_arraylist  = env->GetObjectClass(jarrayList);
  jmethodID arraylist_get  = env->GetMethodID(cls_arraylist, "get", "(I)Ljava/lang/Object;");
  jmethodID arraylist_size = env->GetMethodID(cls_arraylist, "size", "()I");

  jint           len = env->CallIntMethod(jarrayList, arraylist_size);
  std::vector<T> vec;
  for (int i = 0; i < len; i++)
  {
    jobject jobj = env->CallObjectMethod(jarrayList, arraylist_get, i);
    vec.emplace_back(converter(env, jobj));
  }
  return vec;
}

/**
 * java array to cpp vector convert function
 * @tparam T 容器的C++ 对象类型
 * @param env JNIEnv
 * @param jarray java的 []数组 jni指针
 * @param converter 对象转换函数
 * @return std::vector<T>
 */
template<typename T>
static inline std::vector<T> jArray_to_std_vector(JNIEnv *env, jobjectArray jarray, JObj2CppObjConvertFunc<T> converter)
{
  jsize          arraysize = env->GetArrayLength(jarray);
  std::vector<T> vec;
  for (int i = 0; i < arraysize; i++)
  {
    auto jconfig = GetObjectItem(env, jarray, i);
    vec.emplace_back(converter(env, jconfig.get()));
  }
  return vec;
}


/**
 * convert java HashMap<, string> to std::map<string, string>
 * @param env JNIEnv
 * @param jmap java HashMap jni object
 * @return std::map<string, string>
 */

/**
 * convert java HashMap<Key, Value> to std::map<Key, Value>
 * @tparam Key map key
 * @tparam Value map value
 * @param env JNIEnv
 * @param jmap java HashMap jni pointer
 * @param keyConverter key jobj -> cppobj converter
 * @param valueConverter value jobj -> cppobj converter
 * @return std::map<Key, Value>
 */
template<typename Key, typename Value>
static inline std::map<std::string, std::string> jmap_to_std_map(JNIEnv *                      env,
                                                                 jobject                       jmap,
                                                                 JObj2CppObjConvertFunc<Key>   keyConverter,
                                                                 JObj2CppObjConvertFunc<Value> valueConverter)
{
  jclass    jmapclass   = env->GetObjectClass(jmap);
  jmethodID jkeysetmid  = env->GetMethodID(jmapclass, "keySet", "()Ljava/util/Set;");
  jmethodID jgetmid     = env->GetMethodID(jmapclass, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
  jobject   jsetkey     = env->CallObjectMethod(jmap, jkeysetmid);
  jclass    jsetclass   = env->FindClass("java/util/Set");
  jmethodID jtoArraymid = env->GetMethodID(jsetclass, "toArray", "()[Ljava/lang/Object;");

  std::map<Key, Value> cmap;
  auto                 jobjArray = (jobjectArray)env->CallObjectMethod(jsetkey, jtoArraymid);
  if (jobjArray)
  {
    jsize arraysize = env->GetArrayLength(jobjArray);
    for (int i = 0; i < arraysize; i++)
    {
      auto jkey   = GetObjectItem<Key>(env, jobjArray, i);
      auto jvalue = env->CallObjectMethod(jmap, jgetmid, jkey);
      cmap.emplace(keyConverter(env, jkey.get()), valueConverter(env, jvalue));
    }
  }
  return cmap;
}

static inline jstring string_to_jstring(JNIEnv *env, char *pat)
{
  //定义java String类 strClass
  jclass strClass = (env)->FindClass("java/lang/String");
  //获取String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
  jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
  //建立byte数组
  jbyteArray bytes = (env)->NewByteArray(strlen(pat));
  //将char* 转换为byte数组
  (env)->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte *)pat);
  // 设置String, 保存语言类型,用于byte数组转换至String时的参数
  jstring encoding = (env)->NewStringUTF("GB2312");
  //将byte数组转换为java String,并输出
  return (jstring)(env)->NewObject(strClass, ctorID, bytes, encoding);
}


#endif // __ANDROID__

#endif //_JNIHELPER_HPP
