const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles, barang, or peminjaman", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("barang").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").set({
      userId: ALICE_UID,
      kodeBarang: "ELK-001",
      namaBarang: "Proyektor Epson",
      kategori: "Elektronik",
      stokTotal: 4,
      stokTersedia: 4,
      kondisi: "Baik",
      lokasiRak: "Lemari A1",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Authenticated owner can create, update, and list CloudBarang with exact Kotlin payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").set({
      userId: ALICE_UID,
      kodeBarang: "ELK-001",
      namaBarang: "Proyektor Epson EB-X500",
      kategori: "Elektronik",
      stokTotal: 5,
      stokTersedia: 4,
      kondisi: "Baik",
      lokasiRak: "Lemari Elektronik A1",
      deskripsi: "Proyektor kelas",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").update({
      stokTersedia: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost `id` field & future timestamp & stokTersedia > stokTotal
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_2").set({
      id: "barang_2",
      userId: ALICE_UID,
      kodeBarang: "ELK-002",
      namaBarang: "Laptop",
      kategori: "Elektronik",
      stokTotal: 2,
      stokTersedia: 2,
      kondisi: "Baik",
      lokasiRak: "Rak B",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").update({
      stokTersedia: 99,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").update({
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's profile and inventory", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      nama: "Admin Alice",
      email: "alice@sekolah.sch.id",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").set({
      userId: ALICE_UID,
      kodeBarang: "ELK-001",
      namaBarang: "Proyektor",
      kategori: "Elektronik",
      stokTotal: 3,
      stokTersedia: 3,
      kondisi: "Baik",
      lokasiRak: "Rak A",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("barang").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").update({
      stokTersedia: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("CloudPeminjaman enforces parent barang existence and terminal state locking on return", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Reject orphan peminjaman before barang_1 exists
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("peminjaman").doc("pinjam_1").set({
      userId: ALICE_UID,
      kodePeminjaman: "PJM-001",
      barangDocId: "barang_1",
      kodeBarang: "ELK-001",
      namaBarang: "Proyektor",
      peminjamNama: "Rizky",
      jumlah: 1,
      tanggalPinjam: Date.now() - 86400000,
      tanggalKembaliRencana: Date.now() + 86400000,
      status: "Dipinjam",
      denda: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("barang").doc("barang_1").set({
      userId: ALICE_UID,
      kodeBarang: "ELK-001",
      namaBarang: "Proyektor",
      kategori: "Elektronik",
      stokTotal: 3,
      stokTersedia: 2,
      kondisi: "Baik",
      lokasiRak: "Rak A",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("peminjaman").doc("pinjam_1").set({
      userId: ALICE_UID,
      kodePeminjaman: "PJM-001",
      barangDocId: "barang_1",
      kodeBarang: "ELK-001",
      namaBarang: "Proyektor",
      peminjamNama: "Rizky",
      jumlah: 1,
      tanggalPinjam: Date.now() - 86400000,
      tanggalKembaliRencana: Date.now() + 86400000,
      status: "Dipinjam",
      denda: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid return transition
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("peminjaman").doc("pinjam_1").update({
      status: "Dikembalikan",
      tanggalKembaliAktual: Date.now(),
      denda: 0,
      kondisiKembali: "Baik",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: subsequent update on Dikembalikan must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("peminjaman").doc("pinjam_1").update({
      status: "Dipinjam",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
