class Terminalimageviewer < Formula
  desc "Display images in a terminal using block graphic characters"
  homepage "https://github.com/stefanhaustein/TerminalImageViewer"
  url "https://github.com/stefanhaustein/TerminalImageViewer/archive/v1.0.0.tar.gz"
  sha256 "d28c5746d25d83ea707db52b54288c4fc1851c642ae021951967e69296450c8c"
  head "https://github.com/stefanhaustein/TerminalImageViewer.git"
  depends_on "gcc" => :build unless OS.linux?
  depends_on "imagemagick"

  def install
    cd "src/main/cpp" do
      system "make"
      if OS.mac?
        # No expermimental/filesystem.h on mac
        system "#{Formula["gcc"].opt_bin}/gcc-#{Formula["gcc"].version_suffix}", "-std=c++17",
                                                                                  "-Wall",
                                                                                  "-fpermissive",
                                                                                  "-fexceptions",
                                                                                  "-O2", "-c",
                                                                                  "tiv.cpp", "-o", "tiv.o"
        system "#{Formula["gcc"].opt_bin}/gcc-#{Formula["gcc"].version_suffix}", "tiv.o", "-o", 
                                                                                 "tiv", "-lstdc++fs",
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
