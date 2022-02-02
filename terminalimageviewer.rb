class Terminalimageviewer < Formula
  desc "Display images in a terminal using block graphic characters"
  homepage "https://github.com/stefanhaustein/TerminalImageViewer"
  url "https://github.com/stefanhaustein/TerminalImageViewer/archive/refs/tags/v1.1.1.tar.gz"
  sha256 "9a5f5c8688ef8db0e88dfcea6a1ae30da32268a7ab7972ff0de71955a75af0db"
  license "Apache-2.0"
  head "https://github.com/stefanhaustein/TerminalImageViewer.git", branch: "master"

  depends_on "imagemagick"

  def install
    odie "This formula is for macOS 10.14-, please use tiv from homebrew-core instead!" if OS.linux?
    cd "src/main/cpp" do
      system "/usr/local/bin/g++-#{Formula["gcc"].version_suffix}", "-std=c++17", "-Wall", "-fpermissive",
             "-fexceptions", "-O2", "-c", "-L/usr/local/opt/gcc/lib/gcc/#{Formula["gcc"].version_suffix}/", "tiv.cpp",
             "-o", "tiv.o"
      system "/usr/local/bin/g++-#{Formula["gcc"].version_suffix}", "tiv.o", "-o", "tiv",
             "-L/usr/local/opt/gcc/lib/gcc/#{Formula["gcc"].version_suffix}/", "-pthread", "-s"
      bin.install "tiv"
    end
  end

  test do
    assert_equal "\e[48;2;0;0;255m\e[38;2;0;0;255m  \e[0m",
                 shell_output("#{bin}/tiv #{test_fixtures("test.png")}").strip
  end
end