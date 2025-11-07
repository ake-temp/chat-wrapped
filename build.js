import tailwindPlugin from 'bun-plugin-tailwind';

const result = await Bun.build({
  entrypoints: ['./target/app.js'],
  outdir: './target/public',
  minify: true,
  sourcemap: 'external',
  plugins: [tailwindPlugin],
});

if (!result.success) {
  console.error('Build failed');
  for (const message of result.logs) {
    console.error(message);
  }
  process.exit(1);
}
