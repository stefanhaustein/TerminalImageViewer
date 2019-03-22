AC_INIT(tiv, 1.0)
AM_INIT_AUTOMAKE([-Wall -Werror foreign])

dnl Switch to a C++ compiler, and check if it works
AC_LANG(C++)
AC_PROG_CXX

EXTRA_CXXFLAGS="-std=c++17  -Wall -fpermissive -fexceptions -O2"
AC_SUBST([EXTRA_CXXFLAGS])
EXTRA_LDFLAGS="-lstdc++fs -pthread -s"
AC_SUBST([EXTRA_LDFLAGS])

dnl AC_CHECK_HEADERS(CImg.h,, AC_MSG_ERROR([required header file missing]))
AC_SEARCH_LIBS(libstdc++,, AC_MSG_ERROR([required lib file missing]))

AC_CONFIG_FILES([
 Makefile
 src/main/cpp/Makefile
])
dnl Process Makefile.in to create Makefile
AC_OUTPUT
