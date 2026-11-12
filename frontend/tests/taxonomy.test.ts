import { expect, it } from 'vitest';
import { parentChoices } from '../src/shared/taxonomy';
it('不同表的规格与规格值允许同号，只有分类不能选择自身为父级', () => {
  const parents = [{ id: '1', parentId: null, name: '颜色' }];
  expect(parentChoices('specification-values', '1', parents)).toEqual(parents);
  expect(parentChoices('categories', '1', parents)).toEqual([]);
});
