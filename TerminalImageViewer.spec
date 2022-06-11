Name:           TerminalImageViewer
Version:        1.1.1
Release:        1%{?dist}
Summary:        Display images in a terminal using block graphic characters

License:        ASL 2.0
URL:            https://github.com/stefanhaustein/TerminalImageViewer
Source0:        https://github.com/stefanhaustein/TerminalImageViewer/archive/refs/tags/v%{version}.tar.gz

BuildRequires:  g++
BuildRequires:  make
Requires:       ImageMagick-c++

%define debug_package %{nil}

%description
Small C++ program to display images in a (modern) terminal using RGB ANSI codes
and unicode block graphic characters. There are various similar tools (such as
`timg`) using the unicode half block character to display two 24bit pixels per
character cell. This program enhances the resolution by mapping 4x8 pixel cells
to different unicode characters.


%prep
%autosetup


%build
cd ./src/main/cpp
%make_build


%install
cd ./src/main/cpp
%make_install


%files
%{_exec_prefix}/local/bin/tiv
%license LICENSE
%doc README.md



%changelog
* Sat Jun 11 2022 Marko Vejnovic <contact@markovejnovic.com> 1.1.1
- Initialization Error Fix
