"""CLI regression tests; run after `make -C src` with Python 3."""
import pathlib
import subprocess
import tempfile
import unittest


TIV = pathlib.Path(__file__).resolve().parents[1] / "src" / "tiv"


class ThumbnailHeightTests(unittest.TestCase):
    def test_height_limit(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = []
            for name, width, height in [("wide", 160, 40), ("tall", 40, 160),
                                        ("square", 80, 80), ("small", 8, 8)]:
                path = pathlib.Path(directory) / (name + ".ppm")
                path.write_bytes(f"P6\n{width} {height}\n255\n".encode()
                                 + b"\xff\x00\x00" * width * height)
                paths.append(str(path))
            cases = [
                (["-d", paths[0]], 1, 16),
                (paths[:2], 1, 16),
                ([directory], 2, 16),
                (["-c", "1", *paths], 4, 50),
            ]
            for args, rows, uncapped_height in cases:
                for height in [1, 4, 40]:
                    with self.subTest(args=args, height=height):
                        result = subprocess.run(
                            [str(TIV), "-w", "100", "-h", str(height), *args],
                            capture_output=True, text=True, check=True,
                        )
                        image_lines = [line for line in result.stdout.splitlines()
                                       if "\x1b[" in line]
                        # Each row is capped; labels remain below the image.
                        self.assertEqual(len(image_lines),
                                         rows * min(height, uncapped_height))
                        self.assertTrue(any("ppm" in line
                                            for line in result.stdout.splitlines()))

            result = subprocess.run(
                [str(TIV), "-f", "-w", "100", "-h", "4", *paths],
                capture_output=True, text=True, check=True,
            )
            self.assertEqual(sum("\x1b[" in line
                                 for line in result.stdout.splitlines()), 13)


if __name__ == "__main__":
    unittest.main()
