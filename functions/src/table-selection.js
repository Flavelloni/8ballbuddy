export function validateTableSelection(table) {
  if (!table || table.schemaVersion !== 1 || !Array.isArray(table.corners) || table.corners.length !== 4 ||
      !Number.isInteger(table.imageWidth) || table.imageWidth < 1 || table.imageWidth > 20000 ||
      !Number.isInteger(table.imageHeight) || table.imageHeight < 1 || table.imageHeight > 20000 ||
      !/^[0-9a-f]{64}$/.test(table.imageSha256)) throw new Error("Invalid table annotation");
  const corners=table.corners;
  if (!corners.every(p=>p && [p.x,p.y].every(n=>typeof n === "number" && Number.isFinite(n) && n>=0 && n<=1))) throw new Error("Invalid table corners");
  for (let i=0;i<4;i++) {
    const a=corners[i],b=corners[(i+1)%4],c=corners[(i+2)%4];
    if (!a || ![a.x,a.y].every(n=>typeof n === "number" && Number.isFinite(n) && n>=0 && n<=1) ||
        (b.x-a.x)*(c.y-b.y)-(b.y-a.y)*(c.x-b.x)<=.0001) throw new Error("Invalid table corners");
  }
  if (corners[0].y+corners[1].y>=corners[2].y+corners[3].y || corners[0].x+corners[3].x>=corners[1].x+corners[2].x) throw new Error("Invalid table corner order");
}
