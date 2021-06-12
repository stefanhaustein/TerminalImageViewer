class Terminalimageviewer < Formula
  desc "Display images in a terminal using block graphic characters"
  homepage "https://github.com/stefanhaustein/TerminalImageViewer"
  url "https://github.com/stefanhaustein/TerminalImageViewer/archive/v1.1.0.tar.gz"
  sha256 "727ddaa048643cb0a13e23bc308415060d99d8b1853883ad339ad7b952050d61"
  head "https://github.com/stefanhaustein/TerminalImageViewer.git"
  depends_on "gcc" => :build unless OS.linux?
  depends_on "imagemagick"

  def install
    cd "src/main/cpp" do
      if OS.mac?
        # No expermimental/filesystem.h on mac. 
        system "/usr/local/bin/g++-#{Formula["gcc"].version_suffix}", "-std=c++17",
                                                                                  "-Wall",
                                                                                  "-fpermissive",
                                                                                  "-fexceptions",
                                                                                  "-O2", "-c",
                                                                                  "-L/usr/local/opt/gcc/lib/gcc/11/",
                                                                                  "tiv.cpp", "-o", "tiv.o"
        system "/usr/local/bin/g++-#{Formula["gcc"].version_suffix}", "tiv.o", "-o", 
                                                                                 "tiv", "-L/usr/local/opt/gcc/lib/gcc/11/", "-lstdc++fs",
                                                                                 "-pthread", "-s"
      else
        system "make"
      end
      bin.install "tiv"
    end
  end

  test do
    # Downloads a public domain test file from wikimedia commons and displays it.
    # For some reason, when you redirect the output it is blank.
    assert_equal "",
    shell_output("#{bin}/tiv -0 https://upload.wikimedia.org/wikipedia/commons/2/24/Cornell_box.png").strip
  end
end
