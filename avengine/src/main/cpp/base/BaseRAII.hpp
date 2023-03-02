
#ifndef __POINTER_RAII_HDR__
#define __POINTER_RAII_HDR__

#include <memory>
#include <functional>


template<typename T>
static inline void genericFree(T *p)
{
  free(static_cast<void *>(p));
}


template<typename T>
using alloc_unique_ptr = std::unique_ptr<T, PointerDel<genericFree<T>>>;


template<typename TO, typename FROM>
static inline std::unique_ptr<TO> static_unique_pointer_cast(std::unique_ptr<FROM> &&old)
{
  //conversion: unique_ptr<FROM>->FROM*->TO*->unique_ptr<TO>
  return std::unique_ptr<TO> {static_cast<TO *>(old.release())};
}

#if __has_include(<stdio.h>)
#include <stdio.h>
using FilePtr = std::unique_ptr<FILE, PointerDel<fclose>>;
#endif

#if __has_include(<dirent.h>)
#include <dirent.h>
using DirPtr = std::unique_ptr<DIR, PointerDel<closedir>>;
#endif


template<typename T>
using CustomUniquePtr = std::unique_ptr<T, std::function<void(T *)>>;


#endif //__POINTER_RAII_HDR__


