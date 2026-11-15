"""Regression tests for development source and published Stable version rules."""
import importlib.util
import pathlib
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("version_guard", ROOT / "check-version-consistency.py")
MOD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MOD)


class VersionGuardTest(unittest.TestCase):
    def verify(self, source="2.13.0", stable="2.12.2", contract="2.12.2", tag=None,
               mcmod=None, java=None):
        with (
            patch.object(MOD, "read_build_version", return_value=source),
            patch.object(MOD, "read_mcmod_version", return_value=mcmod or source),
            patch.object(MOD, "read_source_version", return_value=java or source),
            patch.object(MOD, "read_readme_stable_version", return_value=stable),
            patch.object(MOD, "read_readme_contract_version", return_value=contract),
            patch("sys.argv", ["check-version-consistency.py"] + (["--tag", tag] if tag else [])),
        ):
            return MOD.main()

    def test_development_version_ahead_of_stable(self):
        self.assertEqual(0, self.verify())

    def test_reject_source_metadata_drift(self):
        self.assertEqual(1, self.verify(mcmod="2.12.2"))
        self.assertEqual(1, self.verify(java="2.12.2"))

    def test_reject_readme_contract_mismatch(self):
        self.assertEqual(1, self.verify(contract="2.13.0"))

    def test_reject_readme_newer_than_source(self):
        self.assertEqual(1, self.verify(source="2.12.2", stable="2.13.0", contract="2.13.0"))

    def test_reject_tag_before_promotion(self):
        self.assertEqual(1, self.verify(tag="v2.13.0"))

    def test_accept_consistent_tag(self):
        self.assertEqual(0, self.verify(stable="2.13.0", contract="2.13.0", tag="v2.13.0"))

    def test_reject_wrong_tag(self):
        self.assertEqual(1, self.verify(stable="2.13.0", contract="2.13.0", tag="v2.12.2"))


if __name__ == "__main__":
    unittest.main()
