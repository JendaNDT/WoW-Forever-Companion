import json,tempfile,unittest,subprocess
from pathlib import Path
from release_identity import source_manifest,manifest_hash,sha,verify_identity,APP_ID

class ReleaseIdentityTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        for name,data in {'app/build.gradle.kts':b'build','app/src/main/java/Test.kt':b'source','app/src/main/assets/content/starter.json':b'{}','app/src/main/assets/content/publisher.der':b'public key','release/owner-certificate.sha256':b'1'*64}.items():
            p=self.root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
        self.meta={'schema':1,'applicationId':APP_ID,'versionCode':12,'versionName':'0.12.0-e12','channel':'pilot','sourceRevision':'a'*40,'sourceDirty':False,'buildInputsSha256':manifest_hash(source_manifest(self.root)),'fallbackCatalogSha256':sha(b'{}'),'publisherKeySha256':sha(b'public key'),'ownerCertificateSha256':'1'*64}
        self.state()
    def state(self):
        (self.root/'source-state.json').write_text(json.dumps({'sourceRevision':self.meta['sourceRevision'],'buildInputsSha256':self.meta['buildInputsSha256']}))
    def test_exact_archive_matches_and_docs_are_outside_apk_inputs(self):
        self.assertFalse(verify_identity(self.meta,self.root)['gitRevisionVerified'])
        (self.root/'README.md').write_text('new instructions');self.assertEqual(self.meta['buildInputsSha256'],verify_identity(self.meta,self.root)['buildInputsSha256'])
    def test_changed_source_cannot_keep_old_apk_identity(self):
        (self.root/'app/src/main/java/Test.kt').write_text('changed')
        with self.assertRaises(ValueError):verify_identity(self.meta,self.root)
    def test_dirty_revision_is_never_a_release(self):
        self.meta['sourceDirty']=True
        with self.assertRaises(ValueError):verify_identity(self.meta,self.root)
    def test_unknown_metadata_fields_and_boolean_version_are_rejected(self):
        for edit in [{'extra':'not supported'},{'versionCode':True},{'schema':True}]:
            with self.subTest(edit=edit),self.assertRaises(ValueError):verify_identity(self.meta|edit,self.root)
    def test_wrong_public_pins_are_rejected_even_when_sources_are_unchanged(self):
        for field in ['publisherKeySha256','ownerCertificateSha256','fallbackCatalogSha256']:
            with self.subTest(field=field),self.assertRaises(ValueError):verify_identity(self.meta|{field:'2'*64},self.root)
    def test_archive_state_cannot_claim_a_different_revision(self):
        (self.root/'source-state.json').write_text(json.dumps({'sourceRevision':'b'*40,'buildInputsSha256':self.meta['buildInputsSha256']}))
        with self.assertRaises(ValueError):verify_identity(self.meta,self.root)
    def test_git_receipt_is_bound_to_actual_committed_inputs(self):
        def git(*args):return subprocess.check_output(['git','-C',str(self.root),*args],stderr=subprocess.DEVNULL).decode().strip()
        git('init','-q');git('config','user.name','Test');git('config','user.email','test@local.invalid');git('add','.');git('commit','-qm','Code')
        self.meta['sourceRevision']=git('rev-parse','HEAD')
        (self.root/'README.md').write_text('documentation only');git('add','.');git('commit','-qm','Docs')
        self.assertTrue(verify_identity(self.meta,self.root)['gitRevisionVerified'])
        # A valid-looking commit hash of different code must not be accepted as the APK's origin.
        (self.root/'app/src/main/java/Test.kt').write_text('new code');git('add','.');git('commit','-qm','Different code')
        self.meta['sourceRevision']=git('rev-parse','HEAD')
        (self.root/'app/src/main/java/Test.kt').write_text('source')
        with self.assertRaises(ValueError):verify_identity(self.meta,self.root)

if __name__=='__main__':unittest.main()
