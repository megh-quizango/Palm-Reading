"""Result cache (READ-04, D-12) and quality pre-gate (D-09)."""

import uuid

from conftest import FakeSdk, client_factory, make_jpeg, poll, post_job


def _hdrs(install: str) -> dict:
    return {"X-Install-Id": install, "Idempotency-Key": str(uuid.uuid4())}


def test_same_photo_same_install_is_cached():
    fake = FakeSdk()
    install = str(uuid.uuid4())
    with client_factory(fake) as c:
        first = poll(c, post_job(c, headers=_hdrs(install)).json()["job_id"])
        r = post_job(c, headers=_hdrs(install))
        assert r.status_code == 202
        again = c.get(f"/v1/palm/jobs/{r.json()['job_id']}").json()
    assert first["status"] == again["status"] == "done"
    assert again["reading"] == first["reading"]
    assert again["job_id"] != first["job_id"]
    assert len(fake.calls) == 1
