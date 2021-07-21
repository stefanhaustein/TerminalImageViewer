class Terminalimageviewer < Formula
  desc "Display images in a terminal using block graphic characters"
  homepage "https://github.com/stefanhaustein/TerminalImageViewer"
  url "https://github.com/stefanhaustein/TerminalImageViewer/archive/v1.1.0.tar.gz"
  sha256 "727ddaa048643cb0a13e23bc308415060d99d8b1853883ad339ad7b952050d61"
  head "https://github.com/stefanhaustein/TerminalImageViewer.git"
  uses_from_macos "curl" => :test
  depends_on "gcc" => :build unless OS.linux?
  depends_on "imagemagick"

  def install
    cd "src/main/cpp" do
      system "make"
      if OS.mac?
        # No expermimental/filesystem.h on mac.
        system "/usr/local/bin/g++-#{Formula["gcc"].version_suffix}", "-std=c++17",
                                                                                  "-Wall",
                                                                                  "-fpermissive",
                                                                                  "-fexceptions",
                                                                                  "-O2", "-c",
                                                                                  "-L/usr/local/opt/gcc/lib/gcc/11/",
                                                                                  "tiv.cpp", "-o", "tiv.o"
        system "/usr/local/bin/g++-#{Formula["gcc"].version_suffix}", "tiv.o", "-o", "tiv",
"-L/usr/local/opt/gcc/lib/gcc/11/", "-pthread", "-s"
      else
        system "make"
      end
      bin.install "tiv"
    end
  end

  test do
    # Downloads a public domain test file from wikimedia commons and displays it.
    # NOTE: Test will fail. Will fix when I get to my mac.
    system "curl -o \"test.png\" https://upload.wikimedia.org/wikipedia/commons/2/24/Cornell_box.png"
    assert_equal "[48;2;0;0;0m[38;2;0;0;0m▄[38;2;26;15;8m▄[38;2;39;28;16m▄[38;2;42;29;17m▄[38;2;41;29;17m▄[38;2;43;30;17m▄[38;2;44;30;17m▄[38;2;43;30;17m▄[38;2;44;31;18m▄[38;2;45;31;18m▄[38;2;46;32;18m▄▄[38;2;46;33;19m▄▄▄[38;2;47;34;19m▄▄[38;2;49;35;20m▄[38;2;49;36;20m▄[38;2;47;35;20m▄[38;2;48;35;20m▄[38;2;49;36;21m▄[38;2;49;37;21m▄▄[38;2;50;38;22m▄[38;2;51;39;22m▄[38;2;50;38;22m▄[38;2;49;37;21m▄[38;2;49;38;22m▄[38;2;50;39;22m▄[38;2;49;38;22m▄▄[38;2;50;39;22m▄[38;2;49;39;22m▄[38;2;50;39;22m▄[38;2;51;40;23m▄[38;2;49;39;22m▄[38;2;50;40;23m▄[38;2;49;39;22m▄[38;2;50;40;23m▄▄▄[38;2;49;40;23m▄[38;2;51;41;23m▄[38;2;49;40;23m▄▄[38;2;49;40;22m▄[38;2;48;40;22m▄[38;2;49;40;23m▄[38;2;49;40;22m▄[38;2;49;40;23m▄[38;2;47;39;22m▄▄▄▄[38;2;46;39;21m▄[38;2;45;38;21m▄[38;2;46;38;21m▄[38;2;45;37;21m▄[38;2;43;36;20m▄[38;2;44;38;20m▄[38;2;42;36;20m▄[38;2;42;35;19m▄▄[38;2;41;35;19m▄▄[38;2;40;34;18m▄[38;2;39;33;18m▄[38;2;40;34;18m▄▄[38;2;38;32;18m▄[38;2;38;32;17m▄[38;2;37;31;17m▄[38;2;22;21;10m▄[38;2;0;0;0m▄[0m
",
    shell_output("#{bin}/tiv -0 -w 400 -h 400 test.png").strip
  end
end
