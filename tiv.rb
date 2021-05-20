class Tiv < Formula
  desc "Display images in a terminal using block graphic characters"
  homepage "https://github.com/stefanhaustein/TerminalImageViewer"
  url "https://github.com/stefanhaustein/TerminalImageViewer/archive/v1.1.0.tar.gz"
  sha256 "727ddaa048643cb0a13e23bc308415060d99d8b1853883ad339ad7b952050d61"
  head "https://github.com/stefanhaustein/TerminalImageViewer.git"
  depends_on "gcc" => :build unless OS.linux?
  depends_on "imagemagick"

  def install
    cd "src/main/cpp" do
      system "make"
      bin.install "tiv"
    end
  end

  test do
    # Downloads a public domain test file from wikimedia commons and displays it.
    # NOTE: Test will fail. Will fix when I get to my mac.
    assert_equal "",
    shell_output("#{bin}/tiv -0 -w 400 -h 400 https://upload.wikimedia.org/wikipedia/commons/2/24/Cornell_box.png").strip
  end
end
