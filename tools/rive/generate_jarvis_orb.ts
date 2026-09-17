import { writeFileSync, mkdirSync } from "node:fs";
import { resolve } from "node:path";
import { RiveFile, hex, PropertyKey } from "@stevysmith/rive-generator";

const OUT_DIR = resolve("../../app/src/main/res/raw");
mkdirSync(OUT_DIR, { recursive: true });

const riv = new RiveFile();
const artboard = riv.addArtboard({
  name: "JARVIS_ORB",
  width: 512,
  height: 512,
});

// The generator currently expects a single node container.
// Keep all visual shapes below this node for deterministic export.
const root = riv.addNode(artboard, {
  name: "OrbRoot",
  x: 256,
  y: 256,
});

// Work around the generator's documented first-shape positioning edge case.
const dummy = riv.addShape(root, { name: "Dummy", x: 0, y: 0 });
riv.addRectangle(dummy, { width: 1, height: 1 });
const dummyFill = riv.addFill(dummy);
riv.addSolidColor(dummyFill, hex("#00000000"));

function circle(
  name: string,
  diameter: number,
  color: string,
): ReturnType<typeof riv.addShape> {
  const shape = riv.addShape(root, { name, x: 0, y: 0 });
  riv.addEllipse(shape, { width: diameter, height: diameter });
  const fill = riv.addFill(shape);
  riv.addSolidColor(fill, hex(color));
  return shape;
}

// JARVIS visual language:
// - cyan outer field for the AI energy envelope
// - deep blue structural rings
// - amber core for the "active" intelligence signal
const outer = circle("OuterField", 360, "#071C2A");
const ringA = circle("RingA", 300, "#083F5A");
const ringB = circle("RingB", 246, "#00B9D4");
const ringC = circle("RingC", 190, "#062333");
const coreHalo = circle("CoreHalo", 136, "#0A657A");
const core = circle("Core", 92, "#FFC94D");

// Initial scales are intentionally kept at 1.0; animations only modulate scale.
const animatedShapes = [outer, ringA, ringB, ringC, coreHalo, core];

type Key = { frame: number; value: number; interpolation?: "linear" | "cubic" };

function keyScale(
  animation: ReturnType<typeof riv.addLinearAnimation>,
  target: ReturnType<typeof riv.addShape>,
  property: typeof PropertyKey.scaleX | typeof PropertyKey.scaleY,
  keys: Key[],
) {
  const keyed = riv.addKeyedObject(animation, target);
  const prop = riv.addKeyedProperty(keyed, property);
  for (const key of keys) {
    riv.addKeyFrameDouble(prop, {
      frame: key.frame,
      value: key.value,
      interpolation: key.interpolation ?? "cubic",
    });
  }
}

// Idle: slow breathing of the complete orb.
{
  const anim = riv.addLinearAnimation(artboard, {
    name: "Idle",
    fps: 60,
    duration: 120,
    loop: "pingPong",
  });
  for (const [i, shape] of animatedShapes.entries()) {
    const min = 0.975 + i * 0.002;
    const max = 1.025 + i * 0.003;
    const keys = [
      { frame: 0, value: min },
      { frame: 60, value: max },
      { frame: 120, value: min },
    ];
    keyScale(anim, shape, PropertyKey.scaleX, keys);
    keyScale(anim, shape, PropertyKey.scaleY, keys);
  }
}

// Listening: larger envelope and brighter central activity.
{
  const anim = riv.addLinearAnimation(artboard, {
    name: "Listening",
    fps: 60,
    duration: 72,
    loop: "pingPong",
  });
  for (const [i, shape] of animatedShapes.entries()) {
    const max = 1.05 + (5 - i) * 0.008;
    const keys = [
      { frame: 0, value: 1.0 },
      { frame: 36, value: max },
      { frame: 72, value: 1.0 },
    ];
    keyScale(anim, shape, PropertyKey.scaleX, keys);
    keyScale(anim, shape, PropertyKey.scaleY, keys);
  }
}

// Thinking: faster rotation-like scale waves across nested rings.
{
  const anim = riv.addLinearAnimation(artboard, {
    name: "Thinking",
    fps: 60,
    duration: 90,
    loop: "loop",
  });
  for (const [i, shape] of animatedShapes.entries()) {
    const wave = i * 0.008;
    const keys = [
      { frame: 0, value: 0.99 + wave },
      { frame: 22, value: 1.04 + wave },
      { frame: 45, value: 0.98 + wave },
      { frame: 68, value: 1.025 + wave },
      { frame: 90, value: 0.99 + wave },
    ];
    keyScale(anim, shape, PropertyKey.scaleX, keys);
    keyScale(anim, shape, PropertyKey.scaleY, keys);
  }
}

// Speaking: pronounced pulse, especially around the amber core.
{
  const anim = riv.addLinearAnimation(artboard, {
    name: "Speaking",
    fps: 60,
    duration: 36,
    loop: "pingPong",
  });
  for (const [i, shape] of animatedShapes.entries()) {
    const max = 1.03 + i * 0.012;
    const keys = [
      { frame: 0, value: 0.99 },
      { frame: 18, value: max },
      { frame: 36, value: 0.99 },
    ];
    keyScale(anim, shape, PropertyKey.scaleX, keys);
    keyScale(anim, shape, PropertyKey.scaleY, keys);
  }
}

// Error: short contraction, then recovery.
{
  const anim = riv.addLinearAnimation(artboard, {
    name: "Error",
    fps: 60,
    duration: 48,
    loop: "oneShot",
  });
  for (const shape of animatedShapes) {
    const keys = [
      { frame: 0, value: 1.0 },
      { frame: 12, value: 0.92 },
      { frame: 24, value: 1.04 },
      { frame: 36, value: 0.97 },
      { frame: 48, value: 1.0 },
    ];
    keyScale(anim, shape, PropertyKey.scaleX, keys);
    keyScale(anim, shape, PropertyKey.scaleY, keys);
  }
}

const out = resolve(OUT_DIR, "jarvis_orb.riv");
writeFileSync(out, riv.export());
console.log(`Generated ${out}`);
